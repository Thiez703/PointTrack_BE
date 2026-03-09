package com.chamcong.controller;

import com.chamcong.common.ApiResponse;
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
import com.chamcong.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Đăng ký tài khoản mới")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request,
                                                               HttpServletRequest httpRequest) {
        AuthResponse response = authService.register(request, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Registration successful", response));
    }

    @Operation(summary = "Đăng nhập bằng Email/Password")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request,
                                                             HttpServletRequest httpRequest) {
        LoginResponse response = authService.login(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    @Operation(summary = "Đổi mật khẩu lần đầu (Force Change)")
    @PutMapping("/password/first-change")
    public ResponseEntity<ApiResponse<FirstChangePasswordResponse>> firstChangePassword(
            @Valid @RequestBody FirstChangePasswordRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader("Authorization") String authHeader,
            HttpServletRequest httpRequest) {
        String currentAccessToken = authHeader.replace("Bearer ", "");
        FirstChangePasswordResponse response = authService.firstChangePassword(
                request, userDetails.getUsername(), currentAccessToken, httpRequest);
        return ResponseEntity.ok(ApiResponse.ok("Password changed successfully", response));
    }

    @Operation(summary = "Quên mật khẩu - gửi email reset link")
    @PostMapping("/password/forgot")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("If the email exists, a reset link has been sent", null));
    }

    @Operation(summary = "Reset mật khẩu bằng token từ email")
    @PutMapping("/password/reset")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok("Password reset successful", null));
    }

    @Operation(summary = "Đổi mật khẩu thủ công (yêu cầu mật khẩu hiện tại)")
    @PutMapping("/password/change")
    public ResponseEntity<ApiResponse<TokenResponse>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestHeader("Authorization") String authHeader,
            HttpServletRequest httpRequest) {
        String currentAccessToken = authHeader.replace("Bearer ", "");
        TokenResponse response = authService.changePasswordWithTokens(
                request, userDetails.getUsername(), currentAccessToken, httpRequest);
        return ResponseEntity.ok(ApiResponse.ok("Password changed successfully", response));
    }

    @Operation(summary = "Refresh Token Rotation")
    @PostMapping("/token/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request,
                                                                    HttpServletRequest httpRequest) {
        TokenResponse response = authService.refreshToken(request, httpRequest);
        return ResponseEntity.ok(ApiResponse.ok("Token refreshed", response));
    }

    @Operation(summary = "Đăng xuất")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@RequestHeader("Authorization") String authHeader,
                                                     @RequestBody(required = false) RefreshTokenRequest request) {
        String accessToken = authHeader.replace("Bearer ", "");
        String refreshToken = request != null ? request.getRefreshToken() : null;
        authService.logout(accessToken, refreshToken);
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully", null));
    }
}
