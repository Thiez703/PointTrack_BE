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

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^0\\d{9}$", message = "SĐT phải 10 chữ số bắt đầu bằng 0")
    String phoneNumber;

    @NotBlank(message = "Mã OTP không được để trống")
    @Pattern(regexp = "^\\d{6}$", message = "Mã OTP gồm 6 chữ số")
    String otp;
}
