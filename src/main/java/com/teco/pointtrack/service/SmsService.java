package com.teco.pointtrack.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Dịch vụ gửi SMS OTP.
 * MVP: log OTP ra console để dev test mà không cần SMS provider thật.
 * Production: tích hợp Twilio / VNPT SMS / Viettel SMS tại đây.
 */
@Service
@Slf4j
public class SmsService {

    /**
     * Gửi OTP qua SMS đến số điện thoại chỉ định.
     *
     * @param phoneNumber số điện thoại nhận OTP (format 0xxxxxxxxx)
     * @param otp         mã 6 chữ số
     */
    public void sendOtp(String phoneNumber, String otp) {
        // TODO: Tích hợp SMS provider thực (Twilio / VNPT / Viettel)
        log.info("📱 [SMS-OTP] Gửi đến {}: mã OTP = {}", phoneNumber, otp);
    }
}
