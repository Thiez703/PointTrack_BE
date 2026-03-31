package com.teco.pointtrack.service;

import com.teco.pointtrack.dto.employee.EmployeeImportRow;
import com.teco.pointtrack.entity.SalaryLevel;
import com.teco.pointtrack.repository.RoleRepository;
import com.teco.pointtrack.repository.SalaryLevelRepository;
import com.teco.pointtrack.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeImportServiceTest {

    @Mock UserRepository        userRepository;
    @Mock RoleRepository        roleRepository;
    @Mock SalaryLevelRepository salaryLevelRepository;
    @Mock PasswordEncoder       passwordEncoder;
    @Mock PasswordService       passwordService;

    @InjectMocks EmployeeImportService service;

    private Map<String, SalaryLevel> levelsByName;

    @BeforeEach
    void setUp() {
        SalaryLevel cap1 = SalaryLevel.builder()
                .id(1L).name("Cấp 1").baseSalary(new BigDecimal("50000")).build();
        levelsByName = new HashMap<>();
        levelsByName.put("cấp 1", cap1);
    }

    private EmployeeImportRow validRow(int rowNum) {
        EmployeeImportRow row = new EmployeeImportRow();
        row.setRowNumber(rowNum);
        row.setFullName("Nguyễn Văn A");
        row.setPhone("0901234567");
        row.setEmail("test@example.com");
        row.setSalaryLevelName("Cấp 1");
        row.setArea("Quận 1");
        return row;
    }

    @Test
    @DisplayName("validateRow: row hợp lệ → trả về null")
    void validateRow_valid() {
        when(userRepository.existsByPhoneNumberAndDeletedAtIsNull("0901234567")).thenReturn(false);
        when(userRepository.existsByEmailAndDeletedAtIsNull("test@example.com")).thenReturn(false);

        String result = service.validateRow(validRow(2), levelsByName,
                new HashSet<>(), new HashSet<>());

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("validateRow: phone không hợp lệ (không bắt đầu bằng 0)")
    void validateRow_invalidPhone_noLeadingZero() {
        EmployeeImportRow row = validRow(2);
        row.setPhone("1901234567"); // không bắt đầu 0

        String result = service.validateRow(row, levelsByName, new HashSet<>(), new HashSet<>());

        assertThat(result).contains("Số điện thoại");
    }

    @Test
    @DisplayName("validateRow: phone không hợp lệ (9 chữ số)")
    void validateRow_invalidPhone_9digits() {
        EmployeeImportRow row = validRow(2);
        row.setPhone("090123456"); // chỉ 9 chữ số

        String result = service.validateRow(row, levelsByName, new HashSet<>(), new HashSet<>());

        assertThat(result).contains("Số điện thoại");
    }

    @Test
    @DisplayName("validateRow: email không hợp lệ")
    void validateRow_invalidEmail() {
        EmployeeImportRow row = validRow(2);
        row.setEmail("not-an-email");

        String result = service.validateRow(row, levelsByName, new HashSet<>(), new HashSet<>());

        assertThat(result).contains("Email");
    }

    @Test
    @DisplayName("validateRow: salaryLevelName không tồn tại trong hệ thống")
    void validateRow_levelNotFound() {
        EmployeeImportRow row = validRow(2);
        row.setSalaryLevelName("Cấp 99");

        String result = service.validateRow(row, levelsByName, new HashSet<>(), new HashSet<>());

        assertThat(result).contains("Cấp 99");
    }

    @Test
    @DisplayName("validateRow: phone đã tồn tại trong DB")
    void validateRow_phoneDuplicateInDb() {
        when(userRepository.existsByPhoneNumberAndDeletedAtIsNull("0901234567")).thenReturn(true);

        String result = service.validateRow(validRow(2), levelsByName, new HashSet<>(), new HashSet<>());

        assertThat(result).contains("Số điện thoại đã tồn tại");
    }

    @Test
    @DisplayName("validateRow: email đã tồn tại trong DB")
    void validateRow_emailDuplicateInDb() {
        when(userRepository.existsByPhoneNumberAndDeletedAtIsNull("0901234567")).thenReturn(false);
        when(userRepository.existsByEmailAndDeletedAtIsNull("test@example.com")).thenReturn(true);

        String result = service.validateRow(validRow(2), levelsByName, new HashSet<>(), new HashSet<>());

        assertThat(result).contains("Email đã tồn tại");
    }

    @Test
    @DisplayName("validateRow: phone trùng trong file (seenPhones)")
    void validateRow_phoneDuplicateInFile() {
        Set<String> seenPhones = new HashSet<>();
        seenPhones.add("0901234567");

        String result = service.validateRow(validRow(2), levelsByName, seenPhones, new HashSet<>());

        assertThat(result).contains("trùng lặp trong file");
    }

    @Test
    @DisplayName("validateRow: email trùng trong file (seenEmails)")
    void validateRow_emailDuplicateInFile() {
        Set<String> seenEmails = new HashSet<>();
        seenEmails.add("test@example.com");

        String result = service.validateRow(validRow(2), levelsByName, new HashSet<>(), seenEmails);

        assertThat(result).contains("trùng lặp trong file");
    }

    @Test
    @DisplayName("validateRow: fullName trống → lỗi")
    void validateRow_emptyFullName() {
        EmployeeImportRow row = validRow(2);
        row.setFullName("  ");

        String result = service.validateRow(row, levelsByName, new HashSet<>(), new HashSet<>());

        assertThat(result).contains("Họ tên");
    }

    @Test
    @DisplayName("validateRow: salaryLevelName null → lỗi")
    void validateRow_nullLevelName() {
        EmployeeImportRow row = validRow(2);
        row.setSalaryLevelName(null);

        String result = service.validateRow(row, levelsByName, new HashSet<>(), new HashSet<>());

        assertThat(result).contains("Cấp bậc");
    }
}
