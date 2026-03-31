package com.teco.pointtrack.service;

import com.teco.pointtrack.dto.employee.EmployeeCreateRequest;
import com.teco.pointtrack.dto.employee.EmployeeResponse;
import com.teco.pointtrack.entity.Role;
import com.teco.pointtrack.entity.SalaryLevel;
import com.teco.pointtrack.entity.User;
import com.teco.pointtrack.entity.enums.UserStatus;
import com.teco.pointtrack.exception.ConflictException;
import com.teco.pointtrack.exception.NotFoundException;
import com.teco.pointtrack.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock UserRepository             userRepository;
    @Mock RoleRepository             roleRepository;
    @Mock SalaryLevelRepository      salaryLevelRepository;
    @Mock SalaryLevelHistoryRepository historyRepository;
    @Mock ShiftRepository            shiftRepository;
    @Mock AttendanceRecordRepository attendanceRepository;
    @Mock PasswordEncoder            passwordEncoder;
    @Mock PasswordService            passwordService;

    @InjectMocks EmployeeService service;

    private static final String PHONE = "0901234567";
    private static final String EMAIL = "test@example.com";

    private EmployeeCreateRequest validRequest() {
        EmployeeCreateRequest req = new EmployeeCreateRequest();
        req.setFullName("Nguyễn Văn A");
        req.setPhone(PHONE);
        req.setEmail(EMAIL);
        req.setSalaryLevelId(null);
        req.setHiredDate("2024-01-15");
        req.setArea("Quận 1");
        req.setSkills(List.of("tam_be", "ve_sinh"));
        return req;
    }

    private SalaryLevel mockSalaryLevel() {
        return SalaryLevel.builder()
                .id(1L)
                .name("Cấp 1")
                .baseSalary(new BigDecimal("50000"))
                .build();
    }

    private Role mockUserRole() {
        Role role = new Role();
        role.setId(1L);
        role.setSlug("USER");
        role.setDisplayName("User");
        return role;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // createEmployee()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createEmployee()")
    class CreateEmployeeTests {

        @BeforeEach
        void defaultMocks() {
            lenient().when(userRepository.findByPhoneNumber(anyString())).thenReturn(Optional.empty());
            lenient().when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
            lenient().when(roleRepository.findBySlug("USER")).thenReturn(Optional.of(mockUserRole()));
            lenient().when(salaryLevelRepository.findByNameAndDeletedAtIsNull("Cấp 1"))
                    .thenReturn(Optional.of(mockSalaryLevel()));
            lenient().when(passwordService.generateTempPassword()).thenReturn("TempPass1X");
            lenient().when(passwordEncoder.encode(anyString())).thenReturn("$2a$bcrypt");
            lenient().when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                if (u.getId() == null) u.setId(42L);
                return u;
            });
        }

        @Test
        @DisplayName("Tạo thành công → trả về EmployeeResponse với id, fullName, phone, email")
        void createEmployee_success() {
            EmployeeResponse result = service.createEmployee(validRequest());

            assertThat(result).isNotNull();
            assertThat(result.getFullName()).isEqualTo("Nguyễn Văn A");
            assertThat(result.getPhone()).isEqualTo(PHONE);
            assertThat(result.getEmail()).isEqualTo(EMAIL);
            assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);

            verify(userRepository).save(any(User.class));
            verify(passwordService).sendTempPasswordEmail(eq(EMAIL), anyString(), anyString(), eq(PHONE));
        }

        @Test
        @DisplayName("Reactivate: Nếu phone đã tồn tại nhưng đã bị xóa (soft-delete) → Khôi phục user")
        void createEmployee_reactivateSoftDeleted() {
            User deletedUser = User.builder()
                    .id(100L)
                    .fullName("Cũ")
                    .phoneNumber(PHONE)
                    .email("old@example.com")
                    .deletedAt(java.time.LocalDateTime.now())
                    .status(UserStatus.INACTIVE)
                    .build();

            when(userRepository.findByPhoneNumber(PHONE)).thenReturn(Optional.of(deletedUser));

            EmployeeCreateRequest req = validRequest();
            EmployeeResponse result = service.createEmployee(req);

            assertThat(result.getId()).isEqualTo(100L);
            assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(deletedUser.getDeletedAt()).isNull();
            assertThat(deletedUser.getFullName()).isEqualTo(req.getFullName());

            verify(userRepository).save(deletedUser);
            verify(passwordService).sendTempPasswordEmail(eq(EMAIL), anyString(), anyString(), eq(PHONE));
        }

        @Test
        @DisplayName("PHONE_ALREADY_EXISTS → ném ConflictException khi phone đang được dùng bởi user ACTIVE")
        void createEmployee_phoneDuplicate() {
            User activeUser = User.builder().id(10L).phoneNumber(PHONE).deletedAt(null).build();
            when(userRepository.findByPhoneNumber(PHONE)).thenReturn(Optional.of(activeUser));

            assertThatThrownBy(() -> service.createEmployee(validRequest()))
                    .isInstanceOf(ConflictException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("EMAIL_ALREADY_EXISTS → ném ConflictException khi email đang được dùng bởi user ACTIVE")
        void createEmployee_emailDuplicate() {
            User activeUser = User.builder().id(10L).email(EMAIL).deletedAt(null).build();
            when(userRepository.findByEmail(EMAIL.toLowerCase())).thenReturn(Optional.of(activeUser));

            assertThatThrownBy(() -> service.createEmployee(validRequest()))
                    .isInstanceOf(ConflictException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("SALARY_LEVEL_NOT_FOUND → ném NotFoundException khi salaryLevelId không tồn tại")
        void createEmployee_salaryLevelNotFound() {
            EmployeeCreateRequest req = validRequest();
            req.setSalaryLevelId(999L);

            when(salaryLevelRepository.findByIdAndDeletedAtIsNull(999L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createEmployee(req))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("skills và area được lưu đúng vào user")
        void createEmployee_skillsAndAreaSaved() {
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

            service.createEmployee(validRequest());

            verify(userRepository).save(captor.capture());
            User saved = captor.getValue();
            assertThat(saved.getArea()).isEqualTo("Quận 1");
            assertThat(saved.getSkills()).containsExactly("tam_be", "ve_sinh");
            assertThat(saved.isFirstLogin()).isTrue();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // deleteEmployee() – soft delete + shift cancellation
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteEmployee()")
    class DeleteEmployeeTests {

        @Test
        @DisplayName("Soft delete: deletedAt được set, status = INACTIVE")
        void softDelete_setsDeletedAt() {
            User user = User.builder().id(1L).fullName("Test").status(UserStatus.ACTIVE).build();
            when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));
            when(shiftRepository.findFutureScheduledByEmployee(eq(1L), any())).thenReturn(List.of());
            when(userRepository.save(any())).thenReturn(user);

            service.deleteEmployee(1L);

            assertThat(user.getDeletedAt()).isNotNull();
            assertThat(user.getStatus()).isEqualTo(UserStatus.INACTIVE);
        }

        @Test
        @DisplayName("Trả về thông báo với số ca đã hủy")
        void softDelete_returnsCancelledShiftCount() {
            User user = User.builder().id(1L).fullName("Test").status(UserStatus.ACTIVE).build();
            when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));

            var shift1 = com.teco.pointtrack.entity.Shift.builder()
                    .id(10L).status(com.teco.pointtrack.entity.enums.ShiftStatus.SCHEDULED).build();
            var shift2 = com.teco.pointtrack.entity.Shift.builder()
                    .id(11L).status(com.teco.pointtrack.entity.enums.ShiftStatus.SCHEDULED).build();
            var shift3 = com.teco.pointtrack.entity.Shift.builder()
                    .id(12L).status(com.teco.pointtrack.entity.enums.ShiftStatus.SCHEDULED).build();

            when(shiftRepository.findFutureScheduledByEmployee(eq(1L), any()))
                    .thenReturn(List.of(shift1, shift2, shift3));
            when(userRepository.save(any())).thenReturn(user);

            String message = service.deleteEmployee(1L);

            assertThat(message).contains("3");
            assertThat(shift1.getStatus()).isEqualTo(com.teco.pointtrack.entity.enums.ShiftStatus.CANCELLED);
            assertThat(shift2.getStatus()).isEqualTo(com.teco.pointtrack.entity.enums.ShiftStatus.CANCELLED);
        }

        @Test
        @DisplayName("EMPLOYEE_NOT_FOUND → NotFoundException khi không tìm thấy user")
        void deleteEmployee_notFound() {
            when(userRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteEmployee(99L))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getEmployeeProfile()")
    class GetEmployeeProfileTests {

        @Test
        @DisplayName("Lấy hồ sơ cá nhân thành công với đầy đủ thống kê")
        void getEmployeeProfile_success() {
            User user = User.builder()
                    .id(123L)
                    .fullName("Nguyễn Văn A")
                    .phoneNumber("0987654321")
                    .email("nguyenvana@company.com")
                    .status(UserStatus.ACTIVE)
                    .role(Role.builder().slug("USER").build())
                    .salaryLevel(SalaryLevel.builder().name("Nhân viên kỹ thuật").build())
                    .startDate(java.time.LocalDate.of(2024, 1, 15))
                    .build();

            com.teco.pointtrack.dto.user.UserDetail userDetail = com.teco.pointtrack.dto.user.UserDetail.builder()
                    .id(123L)
                    .build();

            try (MockedStatic<com.teco.pointtrack.common.AuthUtils> authUtils = mockStatic(com.teco.pointtrack.common.AuthUtils.class)) {
                authUtils.when(com.teco.pointtrack.common.AuthUtils::getUserDetail).thenReturn(userDetail);
                when(userRepository.findByIdAndDeletedAtIsNull(123L)).thenReturn(Optional.of(user));

                // Mock statistics
                when(attendanceRepository.countWorkDaysByUserAndPeriod(eq(123L), any(), any())).thenReturn(22L);
                when(attendanceRepository.sumOtHoursByUserAndPeriod(eq(123L), any(), any())).thenReturn(14.5);
                when(attendanceRepository.countLateCheckinsByUserAndPeriod(eq(123L), any(), any())).thenReturn(2L);
                when(attendanceRepository.countWorkDaysHistoryByUser(eq(123L), any())).thenReturn(List.of(
                        new Object[]{9, 20L},
                        new Object[]{10, 23L},
                        new Object[]{11, 21L},
                        new Object[]{12, 22L},
                        new Object[]{1, 18L},
                        new Object[]{2, 20L}
                ));

                var response = service.getEmployeeProfile();

                assertThat(response.getId()).isEqualTo(123L);
                assertThat(response.getFullName()).isEqualTo("Nguyễn Văn A");
                assertThat(response.getWorkStatistics().getSummary().getTotalWorkDaysThisMonth()).isEqualTo(22L);
                assertThat(response.getWorkStatistics().getSummary().getOtHoursThisMonth()).isEqualTo(14.5);
                assertThat(response.getWorkStatistics().getSummary().getLateDaysThisMonth()).isEqualTo(2L);
                assertThat(response.getWorkStatistics().getHistory()).hasSize(6);
            }
        }
    }
}
