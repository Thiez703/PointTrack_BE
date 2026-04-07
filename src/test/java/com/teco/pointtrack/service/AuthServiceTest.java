package com.teco.pointtrack.service;

import com.teco.pointtrack.dto.auth.AuthResponse;
import com.teco.pointtrack.dto.auth.FirstChangePasswordRequest;
import com.teco.pointtrack.dto.auth.LoginRequest;
import com.teco.pointtrack.entity.Role;
import com.teco.pointtrack.entity.User;
import com.teco.pointtrack.entity.enums.UserStatus;
import com.teco.pointtrack.repository.RoleRepository;
import com.teco.pointtrack.repository.UserRepository;
import com.teco.pointtrack.security.CustomUserDetail;
import com.teco.pointtrack.security.CustomUserDetailsService;
import com.teco.pointtrack.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "app.cookie.secure=false")
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private CaptchaService captchaService;

    private User testUser;
    private final String phone = "0999888777";
    private final String defaultPass = "Test@1234";

    @BeforeEach
    void setUp() {
        when(captchaService.verifyCaptcha(anyString(), any(HttpServletRequest.class))).thenReturn(true);

        Role userRole = roleRepository.findBySlug("USER")
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .slug("USER")
                        .displayName("User")
                        .build()));

        testUser = User.builder()
                .fullName("Test User")
                .phoneNumber(phone)
                .passwordHash(passwordEncoder.encode(defaultPass))
                .status(UserStatus.ACTIVE)
                .isFirstLogin(true)
                .role(userRole)
                .build();
        userRepository.save(testUser);
    }

    @Test
    void testAuthFlow_Success() {
        // 1. Login first time
        LoginRequest loginReq = new LoginRequest();
        loginReq.setContact(phone);
        loginReq.setPassword(defaultPass);
        loginReq.setCaptchaToken("dummy");

        AuthResponse loginRes = authService.login(loginReq, mock(HttpServletRequest.class));
        assertThat(loginRes.isForcePasswordChange()).isTrue();

        // 2. Change password
        String newPass = "NewPass@1234";
        FirstChangePasswordRequest changeReq = new FirstChangePasswordRequest();
        changeReq.setNewPassword(newPass);
        changeReq.setConfirmPassword(newPass);

        AuthResponse changeRes = authService.firstChangePassword(changeReq, testUser.getId());
        assertThat(changeRes.isForcePasswordChange()).isFalse();

        // 3. Verify DB state
        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updatedUser.isFirstLogin()).isFalse();
        assertThat(updatedUser.getPasswordChangedAt()).isNotNull();
        assertThat(passwordEncoder.matches(newPass, updatedUser.getPasswordHash())).isTrue();

        // 4. Login again with new password
        loginReq.setPassword(newPass);
        AuthResponse loginRes2 = authService.login(loginReq, mock(HttpServletRequest.class));
        assertThat(loginRes2.isForcePasswordChange()).isFalse();
    }

    @Test
    void testResetPassword_ClearsFirstLogin() {
        // Assume user uses forgot password instead of first change
        testUser.setResetPasswordToken("token-123");
        testUser.setResetTokenExpiredAt(LocalDateTime.now().plusHours(1));
        userRepository.save(testUser);

        com.teco.pointtrack.dto.auth.ResetPasswordRequest resetReq = new com.teco.pointtrack.dto.auth.ResetPasswordRequest();
        resetReq.setToken("token-123");
        resetReq.setNewPassword("Reset@1234");
        resetReq.setConfirmPassword("Reset@1234");

        authService.resetPassword(resetReq);

        User updatedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updatedUser.isFirstLogin()).isFalse();
        assertThat(updatedUser.getPasswordChangedAt()).isNotNull();
        assertThat(passwordEncoder.matches("Reset@1234", updatedUser.getPasswordHash())).isTrue();
    }
}
