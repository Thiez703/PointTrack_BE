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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SalaryLevelRepository salaryLevelRepository;
    private final PasswordEncoder passwordEncoder;
    private final CaptchaService captchaService;
    private final SmsService smsService;
    private final PasswordService passwordService;
    private final JwtUtils jwtUtils;
    private final CustomUserDetailsService userDetailsService;
    private static final String ROLE_USER  = "USER";
    private static final String ROLE_ADMIN = "ADMIN";

    @Transactional
    public UserDetail createEmployeeAccount(CreateEmployeeRequest request) {
        String phone = request.getPhoneNumber().trim();
        String email = (request.getEmail() != null) ? request.getEmail().trim().toLowerCase() : null;

        // 1. Kiểm tra phone
        Optional<User> existingUserByPhone = userRepository.findByPhoneNumber(phone);
        if (existingUserByPhone.isPresent()) {
            User user = existingUserByPhone.get();
            if (user.getDeletedAt() == null) {
                throw new ConflictException("Số điện thoại đã được sử dụng bởi một nhân viên khác");
            }
            // Nếu đã xóa -> Reactivate
            return reactivateEmployee(user, request);
        }

        // 2. Kiểm tra email (nếu có)
        if (email != null) {
            Optional<User> existingUserByEmail = userRepository.findByEmail(email);
            if (existingUserByEmail.isPresent()) {
                User user = existingUserByEmail.get();
                if (user.getDeletedAt() == null) {
                    throw new ConflictException("Email đã được sử dụng bởi một nhân viên khác");
                }
                // Nếu đã xóa -> Reactivate
                return reactivateEmployee(user, request);
            }
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
                .email(email)
                .phoneNumber(phone)
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

    private UserDetail reactivateEmployee(User user, CreateEmployeeRequest request) {
        String tempPassword = generateTempPassword();

        var userRole = roleRepository.findBySlug(ROLE_USER)
                .orElseThrow(() -> new BadRequestException("Cấu hình lỗi: Role USER chưa được tạo"));

        SalaryLevel salaryLevel = salaryLevelRepository.findByNameAndDeletedAtIsNull("Cấp 1")
                .orElseThrow(() -> new BadRequestException("Hệ thống chưa khởi tạo cấp bậc lương: Cấp 1"));

        LocalDate startDate = null;
        if (request.getStartDate() != null && !request.getStartDate().isBlank()) {
            startDate = parseDate(request.getStartDate());
        }

        // Reset data cho user cũ
        user.setFullName(request.getFullName().trim());
        user.setEmail(request.getEmail() != null ? request.getEmail().trim().toLowerCase() : null);
        user.setPhoneNumber(request.getPhoneNumber()); // Cập nhật lại phone (đề phòng)
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user.setStatus(UserStatus.ACTIVE);
        user.setFirstLogin(true);
        user.setDeletedAt(null);
        user.setStartDate(startDate);
        user.setRole(userRole);
        user.setSalaryLevel(salaryLevel);

        userRepository.save(user);
        log.info("[DEV] Tài khoản cũ được khôi phục: SĐT={} | MK tạm={}", 
            user.getPhoneNumber(), tempPassword);

        return mapToUserDetail(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        if (!captchaService.verifyCaptcha(request.getCaptchaToken(), httpRequest)) {
            throw new BadRequestException("Xác thực Captcha thất bại");
        }
        String contact = request.getContact().trim();
        User user = findUserByContact(contact);

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new CustomAuthenticationException("Tài khoản đã bị vô hiệu hóa");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new CustomAuthenticationException("Thông tin đăng nhập không hợp lệ");
        }

        user.setLastLoginAt(LocalDateTime.now());
        user.setLastLoginIp(getClientIp(httpRequest));
        userRepository.save(user);

        CustomUserDetail userDetails = (CustomUserDetail) userDetailsService.loadUserByUsername(contact);

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

    private User findUserByContact(String contact) {
        // 1. Thử tìm theo Email
        var userByEmail = userRepository.findByEmailAndDeletedAtIsNull(contact);
        if (userByEmail.isPresent()) return userByEmail.get();

        // 2. Thử tìm theo Số điện thoại
        return userRepository.findByPhoneNumberAndDeletedAtIsNull(contact)
                .orElseThrow(() -> new CustomAuthenticationException("Thông tin đăng nhập không hợp lệ"));
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
        user.setPasswordChangedAt(LocalDateTime.now());
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
        String contact = request.getContact().trim();
        userRepository.findByEmailAndDeletedAtIsNull(contact).ifPresent(user -> {
            sendOtp(user);
        });
        userRepository.findByPhoneNumberAndDeletedAtIsNull(contact).ifPresent(user -> {
            sendOtp(user);
        });
        // Luôn trả 200 để không tiết lộ thông tin người dùng
    }

    private void sendOtp(User user) {
        String phone = user.getPhoneNumber();
        if (phone != null) {
            String otp = String.format("%06d", new Random().nextInt(999999));
            user.setOtpCode(otp);
            user.setOtpExpiredAt(LocalDateTime.now().plusMinutes(15));
            user.setOtpVerified(false);
            userRepository.save(user);
            smsService.sendOtp(phone, otp);
        }
    }

    @Transactional
    public VerifyOtpResponse verifyOtp(VerifyOtpRequest request) {
        String contact = request.getContact().trim();
        User user = findUserByContact(contact);

        if (user.getOtpCode() == null || !user.getOtpCode().equals(request.getOtp())) {
            throw new BadRequestException("OTP không đúng");
        }
        if (user.getOtpExpiredAt() == null || LocalDateTime.now().isAfter(user.getOtpExpiredAt())) {
            throw new BadRequestException("OTP đã hết hạn. Vui lòng yêu cầu lại");
        }

        // OTP hợp lệ → tạo reset token, hết hạn 10 phút
        String token = UUID.randomUUID().toString();
        user.setOtpVerified(true);
        user.setOtpCode(null);
        user.setOtpExpiredAt(null);
        user.setResetPasswordToken(token);
        user.setResetTokenExpiredAt(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        return VerifyOtpResponse.builder()
                .resetToken(token)
                .message("Xác thực OTP thành công")
                .build();
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
        user.setFirstLogin(false);
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setResetPasswordToken(null);
        user.setResetTokenExpiredAt(null);
        user.setOtpVerified(null);
        user.setOtpExpiredAt(null);
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
        user.setPasswordChangedAt(LocalDateTime.now());
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
            String newPhone = request.getPhoneNumber().trim();
            boolean phoneConflict = userRepository.findByPhoneNumber(newPhone)
                    .filter(u -> !u.getId().equals(userId))
                    .isPresent();
            if (phoneConflict) {
                throw new ConflictException("Số điện thoại đã được sử dụng");
            }
            user.setPhoneNumber(newPhone);
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
        User user = userRepository.findByPhoneNumberAndDeletedAtIsNull(phoneNumber)
                .orElseThrow(() -> new CustomAuthenticationException("Không tìm thấy người dùng"));

        CustomUserDetail userDetails = (CustomUserDetail) userDetailsService.loadUserByUsername(phoneNumber);

        jwtUtils.revokeToken(refreshToken);

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

    /** BR-05: Mật khẩu ≥8 ký tự, ít nhất 1 chữ hoa, ít nhất 1 chữ số */
    private void validatePasswordPolicy(String password) {
        if (password == null || password.length() < 8) {
            throw new BadRequestException("Mật khẩu tối thiểu 8 ký tự");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new BadRequestException("Mật khẩu phải có ít nhất 1 chữ hoa");
        }
        if (!password.matches(".*\\d.*")) {
            throw new BadRequestException("Mật khẩu phải có ít nhất 1 chữ số");
        }
    }

    private String generateTempPassword() {
        return passwordService.generateTempPassword();
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
                .role(user.getRole() != null ? new RoleDto(user.getRole()) : null)
                .salaryLevelId(user.getSalaryLevel() != null ? user.getSalaryLevel().getId() : null)
                .salaryLevelName(user.getSalaryLevel() != null ? user.getSalaryLevel().getName() : null)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
