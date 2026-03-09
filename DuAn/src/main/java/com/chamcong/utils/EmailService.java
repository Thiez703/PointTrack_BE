package com.chamcong.utils;

import com.chamcong.common.enums.ErrorCode;
import com.chamcong.exception.AppException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendAccountCreatedEmail(String toEmail, String fullName, String tempPassword) {
        String subject = "ChamCong - Tài khoản của bạn đã được tạo";
        String html = buildAccountCreatedHtml(fullName, toEmail, tempPassword);
        sendHtmlEmail(toEmail, subject, html);
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String fullName, String resetLink) {
        String subject = "ChamCong - Đặt lại mật khẩu";
        String html = buildPasswordResetHtml(fullName, resetLink);
        sendHtmlEmail(toEmail, subject, html);
    }

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            throw new AppException(ErrorCode.MAIL_SEND_FAILED);
        }
    }

    private String buildAccountCreatedHtml(String fullName, String email, String tempPassword) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; background: #f4f4f4; margin: 0; padding: 20px; }
                        .container { max-width: 520px; margin: 0 auto; background: #fff; border-radius: 8px; padding: 32px; }
                        .header { text-align: center; padding-bottom: 16px; border-bottom: 2px solid #2563eb; }
                        .header h1 { color: #2563eb; margin: 0; font-size: 22px; }
                        .content { padding: 24px 0; }
                        .credentials { background: #f0f4ff; border-radius: 6px; padding: 16px; margin: 16px 0; }
                        .credentials p { margin: 8px 0; font-size: 14px; }
                        .credentials strong { color: #1e40af; }
                        .warning { color: #dc2626; font-size: 13px; font-weight: 600; margin-top: 16px; }
                        .footer { text-align: center; font-size: 12px; color: #888; padding-top: 16px; border-top: 1px solid #eee; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>ChamCong Home Services</h1>
                        </div>
                        <div class="content">
                            <p>Xin chào <strong>%s</strong>,</p>
                            <p>Tài khoản của bạn đã được tạo. Dưới đây là thông tin đăng nhập:</p>
                            <div class="credentials">
                                <p><strong>Email:</strong> %s</p>
                                <p><strong>Mật khẩu tạm:</strong> %s</p>
                            </div>
                            <p class="warning">⚠ Vui lòng đổi mật khẩu ngay sau khi đăng nhập lần đầu.</p>
                        </div>
                        <div class="footer">
                            <p>&copy; 2026 ChamCong Home Services</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(fullName, email, tempPassword);
    }

    private String buildPasswordResetHtml(String fullName, String resetLink) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: Arial, sans-serif; background: #f4f4f4; margin: 0; padding: 20px; }
                        .container { max-width: 520px; margin: 0 auto; background: #fff; border-radius: 8px; padding: 32px; }
                        .header { text-align: center; padding-bottom: 16px; border-bottom: 2px solid #2563eb; }
                        .header h1 { color: #2563eb; margin: 0; font-size: 22px; }
                        .content { padding: 24px 0; }
                        .btn { display: inline-block; background: #2563eb; color: #fff; padding: 12px 32px; border-radius: 6px; text-decoration: none; font-weight: 600; }
                        .warning { color: #dc2626; font-size: 13px; margin-top: 16px; }
                        .footer { text-align: center; font-size: 12px; color: #888; padding-top: 16px; border-top: 1px solid #eee; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>ChamCong Home Services</h1>
                        </div>
                        <div class="content">
                            <p>Xin chào <strong>%s</strong>,</p>
                            <p>Bạn đã yêu cầu đặt lại mật khẩu. Nhấn nút bên dưới để tiếp tục:</p>
                            <p style="text-align:center; margin: 24px 0;">
                                <a href="%s" class="btn">Đặt lại mật khẩu</a>
                            </p>
                            <p class="warning">⚠ Link này sẽ hết hạn sau 15 phút. Nếu bạn không yêu cầu, hãy bỏ qua email này.</p>
                        </div>
                        <div class="footer">
                            <p>&copy; 2026 ChamCong Home Services</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(fullName, resetLink);
    }
}

