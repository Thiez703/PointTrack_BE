package com.teco.pointtrack.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

/**
 * BR-05: Tạo mật khẩu tạm 10 ký tự (≥1 chữ hoa, ≥1 chữ số) và gửi email.
 */
@Service
@Slf4j
public class PasswordService {

    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS    = "0123456789";
    private static final String ALL_CHARS = UPPERCASE + LOWERCASE + DIGITS;
    private static final int    PASSWORD_LENGTH = 10;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    /** No-args constructor — Spring dùng khi không có JavaMailSender bean */
    public PasswordService() {}

    /** Constructor cho test inject mock JavaMailSender */
    public PasswordService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sinh mật khẩu tạm 10 ký tự.
     * Đảm bảo BR-05: ≥1 ký tự hoa, ≥1 chữ số, tổng độ dài 10.
     */
    public String generateTempPassword() {
        SecureRandom rng = new SecureRandom();
        char[] password = new char[PASSWORD_LENGTH];

        // Đảm bảo có ít nhất 1 uppercase và 1 digit
        password[0] = UPPERCASE.charAt(rng.nextInt(UPPERCASE.length()));
        password[1] = DIGITS.charAt(rng.nextInt(DIGITS.length()));

        // Điền phần còn lại bằng ký tự ngẫu nhiên từ tập hợp đầy đủ
        for (int i = 2; i < PASSWORD_LENGTH; i++) {
            password[i] = ALL_CHARS.charAt(rng.nextInt(ALL_CHARS.length()));
        }

        // Xáo trộn để tránh pattern cố định
        for (int i = PASSWORD_LENGTH - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            char tmp = password[i];
            password[i] = password[j];
            password[j] = tmp;
        }

        return new String(password);
    }

    /**
     * Gửi email thông báo tài khoản mới với mật khẩu tạm.
     * Nếu SMTP chưa cấu hình → chỉ log (không throw exception).
     */
    public void sendTempPasswordEmail(String toEmail, String fullName,
                                      String tempPassword, String phone) {
        if (mailSender == null) {
            log.warn("Mail sender not configured, skipping email notification. [DEV] MK tạm: {} | SĐT: {}",
                    tempPassword, phone);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(toEmail);
            msg.setSubject("Tài khoản hệ thống chấm công - Mật khẩu tạm");
            msg.setText(buildEmailBody(fullName, phone, tempPassword));
            mailSender.send(msg);
            log.info("Đã gửi email mật khẩu tạm tới: {}", toEmail);
        } catch (MailException ex) {
            log.warn("Không thể gửi email tới {} – lý do: {}. [DEV] MK tạm: {} | SĐT: {}",
                    toEmail, ex.getMessage(), tempPassword, phone);
        }
    }

    private String buildEmailBody(String fullName, String phone, String tempPassword) {
        return String.format("""
                Xin chào %s,

                Tài khoản của bạn trên Hệ thống Chấm công đã được tạo.

                Thông tin đăng nhập:
                  SĐT đăng nhập : %s
                  Mật khẩu tạm  : %s

                Vui lòng đăng nhập và đổi mật khẩu ngay lần đầu tiên.

                Trân trọng,
                Hệ thống PointTrack
                """, fullName, phone, tempPassword);
    }
}
