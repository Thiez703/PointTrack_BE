package com.teco.pointtrack.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * POST /api/v1/auth/password/verify-otp
 * FR-04: Xác thực OTP để nhận reset token
 */
@Data
public class VerifyOtpRequest {

    @NotBlank(message = "Thông tin đăng nhập không được để trống")
    String contact;

    @NotBlank(message = "Mã OTP không được để trống")
    @Pattern(regexp = "^\\d{6}$", message = "Mã OTP gồm 6 chữ số")
    String otp;
}
