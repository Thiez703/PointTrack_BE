package com.teco.pointtrack.controller;

import com.teco.pointtrack.common.AuthUtils;
import com.teco.pointtrack.dto.auth.*;
import com.teco.pointtrack.dto.common.ApiResponse;
import com.teco.pointtrack.dto.common.MessageResponse;
import com.teco.pointtrack.dto.user.UserDetail;
import com.teco.pointtrack.service.AuthService;
import com.teco.pointtrack.utils.CookieUtils;
import com.teco.pointtrack.utils.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Module AUTH – căn chỉnh theo SRS FR-01 đến FR-07
 * Base path: /api/v1/auth  (DC-09)
 *
 * Endpoint map:
 *   POST   /api/v1/auth/accounts              FR-01 – Admin tạo tài khoản NV
 *   POST   /api/v1/auth/login                 FR-02 – Đăng nhập
 *   PUT    /api/v1/auth/password/first-change FR-03 – Đổi MK lần đầu (force)
 *   POST   /api/v1/auth/password/forgot       FR-04 – Quên MK → gửi email
 *   PUT    /api/v1/auth/password/reset        FR-04 – Đặt lại MK từ link reset
 *   PUT    /api/v1/auth/password/change       Feature#6 – Đổi MK thủ công
 *   POST   /api/v1/auth/token/refresh         FR-05 – Refresh token (có rotation)
 *   POST   /api/v1/auth/logout                FR-06 – Đăng xuất
 *   GET    /api/v1/auth/profile               FR-07 – Xem hồ sơ
 *   PUT    /api/v1/auth/profile               FR-07 – Sửa hồ sơ
 */
@RestController
@RequestMapping({"/v1/auth", "/auth"})
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Xác thực & Quản lý tài khoản")
public class AuthController {

    private final AuthService authService;
    private final JwtUtils jwtUtils;
    private final CookieUtils cookieUtils;

    // ─────────────────────────────────────────────────────────────────────────
    // FR-01: Admin tạo tài khoản NV
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "[Admin] Tạo tài khoản nhân viên",
               description = "Admin tạo tài khoản NV mới. Hệ thống auto-generate MK tạm và gửi email cho NV.")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/accounts")
    public ResponseEntity<ApiResponse<UserDetail>> createEmployeeAccount(
            @Valid @RequestBody CreateEmployeeRequest request) {

        UserDetail created = authService.createEmployeeAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Tạo tài khoản thành công. Mật khẩu tạm đã gửi qua email."));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FR-02: Đăng nhập
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Đăng nhập",
               description = "Xác thực email/password. Nếu is_first_login=true, FE cần redirect sang trang đổi MK.")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {

        AuthResponse authResponse = authService.login(request, httpRequest);
        cookieUtils.setAuthCookies(response, authResponse.getAccessToken(), authResponse.getRefreshToken());
        return ResponseEntity.ok(authResponse);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FR-03: Đổi MK lần đầu (force change)
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Đổi mật khẩu lần đầu",
               description = "Bắt buộc khi is_first_login=true. Không được dùng lại MK tạm.")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/password/first-change")
    public ResponseEntity<AuthResponse> firstChangePassword(
            @Valid @RequestBody FirstChangePasswordRequest request,
            HttpServletResponse response) {

        Long userId = AuthUtils.getUserDetail().getId();
        AuthResponse authResponse = authService.firstChangePassword(request, userId);
        // Cấp cookie mới sau khi đổi MK
        cookieUtils.setAuthCookies(response, authResponse.getAccessToken(), authResponse.getRefreshToken());
        return ResponseEntity.ok(authResponse);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FR-04: Quên MK + Reset MK
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Quên mật khẩu",
               description = "Gửi OTP 6 chữ số qua SMS (hết hạn 15 phút). " +
                             "Luôn trả 200 để tránh tiết lộ số điện thoại có tồn tại không.")
    @PostMapping("/password/forgot")
    public ResponseEntity<MessageResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        authService.forgotPassword(request);
        return ResponseEntity.ok(
                new MessageResponse("Nếu số điện thoại tồn tại trong hệ thống, mã OTP đã được gửi qua SMS."));
    }

    @Operation(summary = "Xác thực OTP",
               description = "Kiểm tra mã OTP gửi qua SMS. Nếu hợp lệ, trả về reset token để đặt lại mật khẩu.")
    @PostMapping("/password/verify-otp")
    public ResponseEntity<ApiResponse<VerifyOtpResponse>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {

        VerifyOtpResponse result = authService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.success(result, result.getMessage()));
    }

    @Operation(summary = "Đặt lại mật khẩu",
               description = "Xác thực reset token (nhận được sau verify-otp) và cập nhật MK mới.")
    @PutMapping("/password/reset")
    public ResponseEntity<MessageResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        authService.resetPassword(request);
        return ResponseEntity.ok(new MessageResponse("Đặt lại mật khẩu thành công. Vui lòng đăng nhập lại."));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Feature #6: Đổi MK thủ công
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Đổi mật khẩu", description = "NV tự đổi MK trong phần cài đặt cá nhân.")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/password/change")
    public ResponseEntity<MessageResponse> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {

        Long userId = AuthUtils.getUserDetail().getId();
        authService.changePassword(request, userId);
        return ResponseEntity.ok(new MessageResponse("Đổi mật khẩu thành công."));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FR-05: Refresh token (có rotation)
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Làm mới token",
               description = "Dùng refresh token để lấy access token mới. " +
                             "Refresh token cũ bị vô hiệu (rotation). " +
                             "Nếu không gửi body, tự động đọc refreshToken từ cookie.")
    @PostMapping("/token/refresh")
    public ResponseEntity<AuthResponse> refreshToken(
            @RequestBody(required = false) TokenRefreshRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {

        // Ưu tiên body; fallback về cookie (httpOnly — JS không thể đọc)
        String refreshToken = (request != null && request.getRefreshToken() != null)
                ? request.getRefreshToken()
                : extractCookieToken(httpRequest, CookieUtils.REFRESH_TOKEN_COOKIE_NAME);

        AuthResponse authResponse = authService.refreshToken(new TokenRefreshRequest(refreshToken));
        cookieUtils.setAuthCookies(response, authResponse.getAccessToken(), authResponse.getRefreshToken());
        return ResponseEntity.ok(authResponse);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FR-06: Đăng xuất
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Đăng xuất", description = "Thu hồi access + refresh token, xóa cookie.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(
            HttpServletRequest request,
            HttpServletResponse response) {

        String accessToken  = extractBearerToken(request);
        String refreshToken = extractCookieToken(request, CookieUtils.REFRESH_TOKEN_COOKIE_NAME);

        authService.logout(accessToken, refreshToken);
        cookieUtils.deleteAuthCookies(response);
        return ResponseEntity.ok(new MessageResponse("Đăng xuất thành công."));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FR-07: Hồ sơ cá nhân
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Lấy thông tin người dùng hiện tại",
               description = "Trả về thông tin đầy đủ của người dùng đang đăng nhập.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDetail>> getMe() {
        com.teco.pointtrack.dto.user.UserDetail userDetail = AuthUtils.getUserDetail();
        if (userDetail == null) {
            throw new com.teco.pointtrack.exception.SignInRequiredException("SIGN_IN_REQUIRED");
        }
        return ResponseEntity.ok(ApiResponse.success(authService.getProfile(userDetail.getId()), null));
    }

    @Operation(summary = "Xem hồ sơ cá nhân")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/profile")
    public ResponseEntity<UserDetail> getProfile() {
        Long userId = AuthUtils.getUserDetail().getId();
        return ResponseEntity.ok(authService.getProfile(userId));
    }

    @Operation(summary = "Cập nhật hồ sơ cá nhân",
               description = "Chỉ sửa được: phoneNumber, avatarUrl. " +
                             "Không sửa được: email, role, salaryLevel.")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/profile")
    public ResponseEntity<UserDetail> updateProfile(
            @RequestBody UpdateProfileRequest request) {

        Long userId = AuthUtils.getUserDetail().getId();
        return ResponseEntity.ok(authService.updateProfile(request, userId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return jwtUtils.extractBearerToken(header);
    }

    private String extractCookieToken(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) return null;
        for (var cookie : request.getCookies()) {
            if (cookieName.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }
}
