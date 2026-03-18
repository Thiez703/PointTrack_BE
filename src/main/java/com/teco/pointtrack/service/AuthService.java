package com.teco.pointtrack.service;

import com.teco.pointtrack.dto.auth.*;
import com.teco.pointtrack.dto.user.RoleDto;
import com.teco.pointtrack.dto.user.UserDetail;
import com.teco.pointtrack.entity.SalaryLevel;
import com.teco.pointtrack.entity.User;
import com.teco.pointtrack.entity.enums.UserStatus;
import com.teco.pointtrack.exception.BadRequestException;
import com.teco.pointtrack.exception.ConflictException;
import com.teco.pointtrack.exception.CustomAuthenticationException;
import com.teco.pointtrack.exception.NotFoundException;
import com.teco.pointtrack.repository.RoleRepository;
import com.teco.pointtrack.repository.SalaryLevelRepository;
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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SalaryLevelRepository salaryLevelRepository;
    private final PasswordEncoder passwordEncoder;
    private final CaptchaService captchaService;
    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService userDetailsService;
    private static final String ROLE_USER  = "USER";
    private static final String ROLE_ADMIN = "ADMIN";

    @Transactional
    public UserDetail createEmployeeAccount(CreateEmployeeRequest request) {

        if (userRepository.findByPhoneNumberAndDeletedAtIsNull(request.getPhoneNumber()).isPresent()) {
            throw new ConflictException("Số điện thoại đã được sử dụng");
        }

        String tempPassword = generateTempPassword();

        var userRole = roleRepository.findBySlug(ROLE_USER)
                .orElseThrow(() -> new BadRequestException("Cấu hình lỗi: Role USER chưa được tạo"));

        // Luôn mặc định gán Cấp 1 cho nhân viên mới
        SalaryLevel salaryLevel = salaryLevelRepository.findByNameAndDeletedAtIsNull("Cấp 1")
                .orElseThrow(() -> new BadRequestException("Hệ thống chưa khởi tạo cấp bậc lương: Cấp 1"));

        LocalDate startDate = null;
        if (request.getStartDate() != null && !request.getStartDate().isBlank()) {
            startDate = parseDate(request.getStartDate());
        }

        User newUser = User.builder()
                .fullName(request.getFullName().trim())
                .email(request.getEmail() != null ? request.getEmail().trim().toLowerCase() : null)
                .phoneNumber(request.getPhoneNumber())
                .passwordHash(passwordEncoder.encode(tempPassword))
                .status(UserStatus.ACTIVE)
                .isFirstLogin(true)
                .startDate(startDate)
                .role(userRole)
                .salaryLevel(salaryLevel)
                .build();

        userRepository.save(newUser);
        log.info("[DEV] Tài khoản mới được tạo: SĐT={} | MK tạm={} | Cấp bậc={}", 
            newUser.getPhoneNumber(), tempPassword, (salaryLevel != null ? salaryLevel.getName() : "Chưa gán"));

        return mapToUserDetail(newUser);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        if (!captchaService.verifyCaptcha(request.getCaptchaToken(), httpRequest)) {
            throw new BadRequestException("Xác thực Captcha thất bại");
        }
        String phoneNumber = request.getPhoneNumber().trim();
        User user = userRepository.findByPhoneNumberAndDeletedAtIsNull(phoneNumber)
                .orElseThrow(() -> new CustomAuthenticationException("Thông tin đăng nhập không hợp lệ"));

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new CustomAuthenticationException("Tài khoản đã bị vô hiệu hóa");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new CustomAuthenticationException("Thông tin đăng nhập không hợp lệ");
        }

        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(getClientIp(httpRequest));
        userRepository.save(user);

        CustomUserDetail userDetails = (CustomUserDetail) userDetailsService.loadUserByUsername(phoneNumber);

        return AuthResponse.builder()
                .accessToken(jwtUtils.generateAccessToken(userDetails))
                .refreshToken(jwtUtils.generateRefreshToken(userDetails))
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole() != null ? user.getRole().getSlug() : null)
                .forcePasswordChange(user.isFirstLogin())
                .build();
    }

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

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setFirstLogin(false);
        userRepository.save(user);

        CustomUserDetail userDetails = (CustomUserDetail) userDetailsService.loadUserByUsername(user.getPhoneNumber());

        return AuthResponse.builder()
                .accessToken(jwtUtils.generateAccessToken(userDetails))
                .refreshToken(jwtUtils.generateRefreshToken(userDetails))
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole() != null ? user.getRole().getSlug() : null)
                .forcePasswordChange(false)
                .build();
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        userRepository.findByEmailAndDeletedAtIsNull(email).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            user.setResetPasswordToken(token);
            user.setResetTokenExpiredAt(LocalDateTime.now().plusMinutes(15));
            userRepository.save(user);
            log.info("[DEV] Reset token: email={} | token={}", email, token);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByResetPasswordToken(request.getToken())
                .orElseThrow(() -> new BadRequestException("Link đặt lại mật khẩu không hợp lệ hoặc đã hết hạn"));

        if (user.getResetTokenExpiredAt() == null || LocalDateTime.now().isAfter(user.getResetTokenExpiredAt())) {
            throw new BadRequestException("Link đặt lại mật khẩu đã hết hạn");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Mật khẩu xác nhận không khớp");
        }

        validatePasswordPolicy(request.getNewPassword());

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setResetPasswordToken(null);
        user.setResetTokenExpiredAt(null);
        userRepository.save(user);
    }

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

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public UserDetail getProfile(Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", userId));
        return mapToUserDetail(user);
    }

    @Transactional
    public UserDetail updateProfile(UpdateProfileRequest request, Long userId) {
        User user = findActiveUser(userId);

        if (request.getPhoneNumber() != null) {
            boolean phoneConflict = userRepository
                    .findByPhoneNumberAndDeletedAtIsNull(request.getPhoneNumber())
                    .filter(u -> !u.getId().equals(userId))
                    .isPresent();
            if (phoneConflict) {
                throw new ConflictException("Số điện thoại đã được sử dụng");
            }
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());

        userRepository.save(user);
        return mapToUserDetail(user);
    }

    public AuthResponse refreshToken(TokenRefreshRequest request) {
        String refreshToken = request.getRefreshToken();
        if (refreshToken == null || !jwtUtils.validateToken(refreshToken)) {
            throw new CustomAuthenticationException("Refresh token không hợp lệ hoặc đã hết hạn");
        }

        String phoneNumber = jwtUtils.extractUsername(refreshToken);
        CustomUserDetail userDetails = (CustomUserDetail) userDetailsService.loadUserByUsername(phoneNumber);

        jwtUtils.revokeToken(refreshToken);

        return AuthResponse.builder()
                .accessToken(jwtUtils.generateAccessToken(userDetails))
                .refreshToken(jwtUtils.generateRefreshToken(userDetails))
                .build();
    }

    public void logout(String accessToken, String refreshToken) {
        if (accessToken != null) {
            try { jwtUtils.revokeToken(accessToken); } catch (Exception ignored) {}
        }
        if (refreshToken != null) {
            try { jwtUtils.revokeToken(refreshToken); } catch (Exception ignored) {}
        }
    }

    private LocalDate parseDate(String dateStr) {
        String[] formats = {"dd/MM/yyyy", "yyyy-MM-dd", "dd-MM-yyyy", "yyyy/MM/dd"};
        for (String format : formats) {
            try {
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(format));
            } catch (DateTimeParseException ignored) {}
        }
        throw new BadRequestException("Định dạng ngày tháng không hợp lệ (hỗ trợ dd/MM/yyyy, yyyy-MM-dd, dd-MM-yyyy): " + dateStr);
    }

    private void validatePasswordPolicy(String password) {
        if (password == null || !password.matches("^\\d{6}$")) {
            throw new BadRequestException("Mật khẩu phải bao gồm đúng 6 chữ số");
        }
    }

    private String generateTempPassword() {
        int digits = (int)(Math.random() * 900_000) + 100_000;
        return String.valueOf(digits);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isBlank()) {
            return xfHeader.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private User findActiveUser(Long id) {
        return userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy nhân viên ID=" + id));
    }

    private UserDetail mapToUserDetail(User user) {
        return UserDetail.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus())
                .role(user.getRole() != null ? user.getRole().getSlug() : null)
                .roleName(user.getRole() != null ? user.getRole().getDisplayName() : null)
                .salaryLevelId(user.getSalaryLevel() != null ? user.getSalaryLevel().getId() : null)
                .salaryLevelName(user.getSalaryLevel() != null ? user.getSalaryLevel().getName() : null)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
