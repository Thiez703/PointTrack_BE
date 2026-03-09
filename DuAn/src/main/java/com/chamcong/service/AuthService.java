package com.chamcong.service;

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
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest);

    LoginResponse login(LoginRequest request, HttpServletRequest httpRequest);

    TokenResponse refreshToken(RefreshTokenRequest request, HttpServletRequest httpRequest);

    void logout(String accessToken, String refreshTokenHash);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(PasswordResetRequest request);

    void changePassword(ChangePasswordRequest request, String email);

    FirstChangePasswordResponse firstChangePassword(FirstChangePasswordRequest request,
                                                     String email,
                                                     String currentAccessToken,
                                                     HttpServletRequest httpRequest);

    TokenResponse changePasswordWithTokens(ChangePasswordRequest request,
                                            String email,
                                            String currentAccessToken,
                                            HttpServletRequest httpRequest);
}
