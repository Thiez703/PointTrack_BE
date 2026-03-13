package com.teco.pointtrack.service;

import com.teco.pointtrack.dto.auth.*;
import com.teco.pointtrack.dto.user.RoleDto;
import com.teco.pointtrack.dto.user.UserDetail;
import com.teco.pointtrack.entity.User;
import com.teco.pointtrack.entity.enums.UserStatus;
import com.teco.pointtrack.exception.BadRequestException;
import com.teco.pointtrack.exception.ConflictException;
import com.teco.pointtrack.exception.CustomAuthenticationException;
import com.teco.pointtrack.exception.NotFoundException;
import com.teco.pointtrack.repository.RoleRepository;
import com.teco.pointtrack.repository.UserRepository;
import com.teco.pointtrack.security.CustomUserDetail;
import com.teco.pointtrack.security.CustomUserDetailsService;
import com.teco.pointtrack.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final CaptchaService captchaService;
    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService userDetailsService;

    private static final String ROLE_USER  = "USER";
    private static final String ROLE_ADMIN = "ADMIN";

    // ─────────────────────────────────────────────────────────────────────────
    // FR-01: Admin tạo tài khoản NV
    // POST /api/v1/auth/accounts
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public UserDetail createEmployeeAccount(CreateEmployeeRequest request) {

        // BR-23: email duy nhất toàn hệ thống
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email đã được sử dụng");
        }

        // BR-02: auto-generate MK tạm đủ policy
        String tempPassword = generateTempPassword();

        var userRole = roleRepository.findBySlug(ROLE_USER)
                .orElseThrow(() -> new BadRequestException("Cấu hình lỗi: Role USER chưa được tạo"));

        LocalDate startDate = null;
        if (request.getStartDate() != null && !request.getStartDate().isBlank()) {
            startDate = LocalDate.parse(request.getStartDate());
        }

        User newUser = User.builder()
                .fullName(request.getFullName().trim())
                .email(request.getEmail().trim().toLowerCase())
                .phoneNumber(request.getPhoneNumber())
                .passwordHash(passwordEncoder.encode(tempPassword))
                .status(UserStatus.ACTIVE)
                .isFirstLogin(true)  // BR-02: bắt buộc đổi MK lần đầu
                .startDate(startDate)
                .role(userRole)
                .build();

        userRepository.save(newUser);

        // TODO: Gửi email chứa MK tạm qua EmailService
        // emailService.sendTempPassword(newUser.getEmail(), tempPassword);
        log.info("[DEV] Tài khoản mới được tạo: email={}", newUser.getEmail());

        return mapToUserDetail(newUser);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FR-02: Đăng nhập bằng email/password
    // POST /api/v1/auth/login
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {

        if (!captchaService.verifyCaptcha(request.getCaptchaToken(), httpRequest)) {
            throw new BadRequestException("Xác thực Captcha thất bại");
        }

        String email = request.getEmail().trim().toLowerCase();

        // Thông báo lỗi chung để tránh user enumeration attack
        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new CustomAuthenticationException("Thông tin đăng nhập không hợp lệ"));

        // Kiểm tra trạng thái trước khi verify password (FR-02)
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new CustomAuthenticationException("Tài khoản đã bị vô hiệu hóa");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new CustomAuthenticationException("Thông tin đăng nhập không hợp lệ");
        }

        // FR-02: ghi nhận last_login_at và IP
        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(getClientIp(httpRequest));
        userRepository.save(user);

        CustomUserDetail userDetails = (CustomUserDetail) userDetailsService.loadUserByUsername(email);

        return AuthResponse.builder()
                .accessToken(jwtUtils.generateAccessToken(userDetails))
                .refreshToken(jwtUtils.generateRefreshToken(userDetails))
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole() != null ? user.getRole().getSlug() : null)
                // BR-02: nếu isFirstLogin = true → FE redirect sang trang đổi MK
                .forcePasswordChange(user.isFirstLogin())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FR-03: Đổi MK lần đầu (Force Change)
    // PUT /api/v1/auth/password/first-change
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public AuthResponse firstChangePassword(FirstChangePasswordRequest request, Long userId) {

        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", userId));

        if (!user.isFirstLogin()) {
            throw new BadRequestException("Tài khoản đã đổi mật khẩu lần đầu");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Mật khẩu xác nhận không khớp");
        }

        validatePasswordPolicy(request.getNewPassword());

        // FR-03: không dùng lại MK tạm
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Không được dùng lại mật khẩu tạm");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setFirstLogin(false);  // tắt flag
        userRepository.save(user);

        // FR-03: cấp token mới sau khi đổi MK
        CustomUserDetail userDetails = (CustomUserDetail) userDetailsService.loadUserByUsername(user.getEmail());

        return AuthResponse.builder()
                .accessToken(jwtUtils.generateAccessToken(userDetails))
                .refreshToken(jwtUtils.generateRefreshToken(userDetails))
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole() != null ? user.getRole().getSlug() : null)
                .forcePasswordChange(false)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FR-04: Quên mật khẩu – gửi link reset
    // POST /api/v1/auth/password/forgot
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        // FR-04: KHÔNG tiết lộ email có tồn tại hay không → luôn return 200
        userRepository.findByEmailAndDeletedAtIsNull(email).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            user.setResetPasswordToken(token);
            user.setResetTokenExpiredAt(LocalDateTime.now().plusMinutes(15)); // hết hạn 15p
            userRepository.save(user);

            // TODO: Gửi email chứa link reset qua EmailService
            // emailService.sendResetPasswordEmail(email, token);
            log.info("[DEV] Reset token: email={} | token={} | expired=15min", email, token);
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FR-04: Đặt lại mật khẩu từ link reset
    // PUT /api/v1/auth/password/reset
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {

        User user = userRepository.findByResetPasswordToken(request.getToken())
                .orElseThrow(() -> new BadRequestException("Link đặt lại mật khẩu không hợp lệ hoặc đã hết hạn"));

        if (user.getResetTokenExpiredAt() == null || LocalDateTime.now().isAfter(user.getResetTokenExpiredAt())) {
            throw new BadRequestException("Link đặt lại mật khẩu đã hết hạn. Vui lòng yêu cầu lại.");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Mật khẩu xác nhận không khớp");
        }

        validatePasswordPolicy(request.getNewPassword());

        // FR-04: không dùng lại MK hiện tại
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Mật khẩu mới không được trùng mật khẩu hiện tại");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        // FR-04: vô hiệu hoá reset token sau khi dùng
        user.setResetPasswordToken(null);
        user.setResetTokenExpiredAt(null);
        userRepository.save(user);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Feature #6: Đổi mật khẩu thủ công
    // PUT /api/v1/auth/password/change
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public void changePassword(ChangePasswordRequest request, Long userId) {

        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", userId));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Mật khẩu hiện tại không đúng");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Mật khẩu xác nhận không khớp");
        }

        validatePasswordPolicy(request.getNewPassword());

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Mật khẩu mới không được trùng mật khẩu hiện tại");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FR-07: Xem/Sửa hồ sơ cá nhân
    // GET + PUT /api/v1/auth/profile
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public UserDetail getProfile(Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", userId));
        return mapToUserDetail(user);
    }

    @Transactional
    public UserDetail updateProfile(UpdateProfileRequest request, Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", userId));

        // FR-07: chỉ cho sửa phoneNumber và avatarUrl
        if (request.getPhoneNumber() != null) {
            boolean phoneConflict = userRepository
                    .findByPhoneNumberAndDeletedAtIsNull(request.getPhoneNumber())
                    .filter(u -> !u.getId().equals(userId))
                    .isPresent();
            if (phoneConflict) {
                throw new ConflictException("PHONE_NUMBER_CONFLICT");
            }
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getAvatarUrl() != null)   user.setAvatarUrl(request.getAvatarUrl());

        userRepository.save(user);
        return mapToUserDetail(user);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FR-05: Refresh Token (với rotation)
    // POST /api/v1/auth/token/refresh
    // ─────────────────────────────────────────────────────────────────────────

    public AuthResponse refreshToken(TokenRefreshRequest request) {

        String refreshToken = request.getRefreshToken();

        if (refreshToken == null || !jwtUtils.validateToken(refreshToken)) {
            throw new CustomAuthenticationException("Refresh token không hợp lệ hoặc đã hết hạn");
        }

        String email = jwtUtils.extractUsername(refreshToken);
        CustomUserDetail userDetails = (CustomUserDetail) userDetailsService.loadUserByUsername(email);

        // FR-05: rotation – thu hồi refresh token cũ
        jwtUtils.revokeToken(refreshToken);

        // Cấp access + refresh token mới
        return AuthResponse.builder()
                .accessToken(jwtUtils.generateAccessToken(userDetails))
                .refreshToken(jwtUtils.generateRefreshToken(userDetails))
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FR-06: Đăng xuất
    // POST /api/v1/auth/logout
    // ─────────────────────────────────────────────────────────────────────────

    public void logout(String accessToken, String refreshToken) {
        if (accessToken != null) {
            try { jwtUtils.revokeToken(accessToken); } catch (Exception ignored) {}
        }
        if (refreshToken != null) {
            try { jwtUtils.revokeToken(refreshToken); } catch (Exception ignored) {}
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * BR-05: Mật khẩu tối thiểu 8 ký tự, ít nhất 1 chữ hoa và 1 chữ số
     */
    private void validatePasswordPolicy(String password) {
        if (password.length() < 8) {
            throw new BadRequestException("Mật khẩu phải có ít nhất 8 ký tự");
        }
        if (password.chars().noneMatch(Character::isUpperCase)) {
            throw new BadRequestException("Mật khẩu phải có ít nhất 1 chữ hoa");
        }
        if (password.chars().noneMatch(Character::isDigit)) {
            throw new BadRequestException("Mật khẩu phải có ít nhất 1 chữ số");
        }
    }

    /**
     * BR-02: auto-generate MK tạm đủ policy BR-05
     * Format: Pt + 6 chữ số → VD: Pt482910
     */
    private String generateTempPassword() {
        int digits = (int)(Math.random() * 900_000) + 100_000;
        return "Pt" + digits;
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isBlank()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private UserDetail mapToUserDetail(User user) {
        return UserDetail.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus())
                .role(user.getRole() != null ? new RoleDto(user.getRole()) : null)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
