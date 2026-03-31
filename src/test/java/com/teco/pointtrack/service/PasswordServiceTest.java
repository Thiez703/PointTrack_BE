package com.teco.pointtrack.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PasswordServiceTest {

    @Mock
    JavaMailSender mailSender;

    PasswordService service;

    @BeforeEach
    void setUp() {
        service = new PasswordService(mailSender);
    }

    @Test
    @DisplayName("generateTempPassword: độ dài phải đúng 10 ký tự")
    void length_is_10() {
        String pwd = service.generateTempPassword();
        assertThat(pwd).hasSize(10);
    }

    @RepeatedTest(50)
    @DisplayName("generateTempPassword: BR-05 – phải có ít nhất 1 chữ hoa và 1 chữ số")
    void brO5_policy_met() {
        String pwd = service.generateTempPassword();
        boolean hasUpper  = pwd.chars().anyMatch(Character::isUpperCase);
        boolean hasDigit  = pwd.chars().anyMatch(Character::isDigit);
        assertThat(hasUpper).as("phải có ít nhất 1 chữ hoa").isTrue();
        assertThat(hasDigit).as("phải có ít nhất 1 chữ số").isTrue();
    }

    @Test
    @DisplayName("generateTempPassword: chỉ chứa ký tự chữ và số (không ký tự đặc biệt)")
    void only_alphanumeric() {
        for (int i = 0; i < 100; i++) {
            String pwd = service.generateTempPassword();
            assertThat(pwd).matches("[A-Za-z0-9]+");
        }
    }

    @Test
    @DisplayName("generateTempPassword: các lần gọi trả về giá trị khác nhau")
    void generates_unique_passwords() {
        String p1 = service.generateTempPassword();
        String p2 = service.generateTempPassword();
        // Xác suất trùng là cực kỳ thấp nhưng về lý thuyết có thể xảy ra
        // Kiểm tra ít nhất 2 trong 10 lần gọi khác nhau
        long distinct = java.util.stream.Stream
                .generate(service::generateTempPassword)
                .limit(10)
                .distinct()
                .count();
        assertThat(distinct).isGreaterThan(1);
    }
}
