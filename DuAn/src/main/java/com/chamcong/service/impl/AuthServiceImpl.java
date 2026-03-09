package com.chamcong.service.impl;

import com.chamcong.common.enums.ErrorCode;
import com.chamcong.common.enums.Role;
import com.chamcong.dto.request.ChangePasswordRequest;
import com.chamcong.dto.request.FirstChangePasswordRequest;
import com.chamcong.dto.request.ForgotPasswordRequest;
import com.chamcong.dto.request.LoginRequest;
import com.chamcong.dto.request.PasswordResetRequest;
import com.chamcong.dto.request.RefreshTokenRequest;
import com.chamcong.dto.request.RegisterRequest;
import com.chamcong.dto.response.AuthResponse;
import com.chamcong.dto.response.FirstChangePasswordResponse;
import com.chamcong.dto.response.LoginResponse;
import com.chamcong.dto.response.TokenResponse;
import com.chamcong.exception.AppException;
import com.chamcong.mapper.UserMapper;
import com.chamcong.model.PasswordResetToken;
import com.chamcong.model.RefreshToken;
import com.chamcong.model.User;
import com.chamcong.repository.PasswordResetTokenRepository;
import com.chamcong.repository.RefreshTokenRepository;
import com.chamcong.repository.UserRepository;
import com.chamcong.security.JwtTokenProvider;
import com.chamcong.service.AuthService;
import com.chamcong.utils.EmailService;
import com.chamcong.utils.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final EmailService emailService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .role(Role.EMPLOYEE)
                .isFirstLogin(true)
                .isActive(true)
                .build();

        user = userRepository.save(user);

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String rawRefreshToken = jwtTokenProvider.generateRefreshToken();
        saveRefreshToken(user, rawRefreshToken, httpRequest);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessExpiration() / 1000)
                .user(userMapper.toUserResponse(user))
                .build();
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmailAndIsActiveTrue(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_DISABLED));

        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(IpUtils.getClientIp(httpRequest));
        userRepository.save(user);

        refreshTokenRepository.revokeAllByUserId(user.getId());

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String rawRefreshToken = jwtTokenProvider.generateRefreshToken();
        saveRefreshToken(user, rawRefreshToken, httpRequest);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .expiresIn(jwtTokenProvider.getAccessExpiration() / 1000)
                .user(userMapper.toUserInfoResponse(user))
                .forcePasswordChange(Boolean.TRUE.equals(user.getIsFirstLogin()))
                .build();
    }

    @Override
    @Transactional
    public FirstChangePasswordResponse firstChangePassword(FirstChangePasswordRequest request,
                                                            String email,
                                                            String currentAccessToken,
                                                            HttpServletRequest httpRequest) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!Boolean.TRUE.equals(user.getIsFirstLogin())) {
            throw new AppException(ErrorCode.NOT_FIRST_LOGIN);
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_MISMATCH);
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.PASSWORD_SAME_AS_CURRENT);
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setIsFirstLogin(false);
        userRepository.save(user);

        refreshTokenRepository.revokeAllByUserId(user.getId());
        jwtTokenProvider.blacklistToken(currentAccessToken);

        String newAccessToken = jwtTokenProvider.generateAccessToken(user);
        String newRawRefreshToken = jwtTokenProvider.generateRefreshToken();
        saveRefreshToken(user, newRawRefreshToken, httpRequest);

        return FirstChangePasswordResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRawRefreshToken)
                .message("Password changed successfully")
                .build();
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmailAndIsActiveTrue(request.getEmail()).ifPresent(user -> {
            passwordResetTokenRepository.invalidateAllByUserId(user.getId());

            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .user(user)
                    .token(token)
                    .expiresAt(LocalDateTime.now().plusMinutes(15))
                    .isUsed(false)
                    .build();
            passwordResetTokenRepository.save(resetToken);

            String resetLink = frontendUrl + "/reset-password?token=" + token;
            emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), resetLink);
        });
    }

    @Override
    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_MISMATCH);
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_RESET_TOKEN));

        if (resetToken.getIsUsed()) {
            throw new AppException(ErrorCode.PASSWORD_RESET_TOKEN_USED);
        }

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.PASSWORD_RESET_TOKEN_EXPIRED);
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setIsUsed(true);
        passwordResetTokenRepository.save(resetToken);

        refreshTokenRepository.revokeAllByUserId(user.getId());
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.INVALID_OLD_PASSWORD);
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_MISMATCH);
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.PASSWORD_SAME_AS_CURRENT);
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        if (Boolean.TRUE.equals(user.getIsFirstLogin())) {
            user.setIsFirstLogin(false);
        }
        userRepository.save(user);
    }

    @Override
    @Transactional
    public TokenResponse changePasswordWithTokens(ChangePasswordRequest request,
                                                   String email,
                                                   String currentAccessToken,
                                                   HttpServletRequest httpRequest) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.INVALID_OLD_PASSWORD);
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_MISMATCH);
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.PASSWORD_SAME_AS_CURRENT);
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        if (Boolean.TRUE.equals(user.getIsFirstLogin())) {
            user.setIsFirstLogin(false);
        }
        userRepository.save(user);

        refreshTokenRepository.revokeAllByUserId(user.getId());
        jwtTokenProvider.blacklistToken(currentAccessToken);

        String newAccessToken = jwtTokenProvider.generateAccessToken(user);
        String newRawRefreshToken = jwtTokenProvider.generateRefreshToken();
        saveRefreshToken(user, newRawRefreshToken, httpRequest);

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessExpiration() / 1000)
                .build();
    }

    @Override
    @Transactional
    public TokenResponse refreshToken(RefreshTokenRequest request, HttpServletRequest httpRequest) {
        String tokenHash = hashToken(request.getRefreshToken());

        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new AppException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        if (storedToken.getIsRevoked()) {
            throw new AppException(ErrorCode.TOKEN_REVOKED);
        }

        if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.TOKEN_EXPIRED);
        }

        storedToken.setIsRevoked(true);
        refreshTokenRepository.save(storedToken);

        User user = storedToken.getUser();
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String newRawRefreshToken = jwtTokenProvider.generateRefreshToken();
        saveRefreshToken(user, newRawRefreshToken, httpRequest);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getAccessExpiration() / 1000)
                .build();
    }

    @Override
    @Transactional
    public void logout(String accessToken, String refreshToken) {
        jwtTokenProvider.blacklistToken(accessToken);

        if (refreshToken != null && !refreshToken.isBlank()) {
            String tokenHash = hashToken(refreshToken);
            refreshTokenRepository.findByTokenHash(tokenHash)
                    .ifPresent(rt -> {
                        rt.setIsRevoked(true);
                        refreshTokenRepository.save(rt);
                    });
        }
    }

    private void saveRefreshToken(User user, String rawToken, HttpServletRequest httpRequest) {
        long refreshExpirationMs = jwtTokenProvider.getRefreshExpiration();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(hashToken(rawToken))
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000))
                .isRevoked(false)
                .ipAddress(IpUtils.getClientIp(httpRequest))
                .build();
        refreshTokenRepository.save(refreshToken);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}

