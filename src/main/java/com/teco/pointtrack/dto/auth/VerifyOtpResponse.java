package com.teco.pointtrack.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * Response cho POST /api/v1/auth/password/verify-otp
 * FR-04: Trả về reset token sau khi xác thực OTP thành công
 */
@Data
@Builder
@AllArgsConstructor
public class VerifyOtpResponse {
    private String resetToken;
    private String message;
}
