package com.teco.pointtrack.config;

import com.teco.pointtrack.entity.*;
import com.teco.pointtrack.entity.enums.*;
import com.teco.pointtrack.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final SalaryLevelRepository salaryLevelRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final CustomerRepository customerRepository;
    private final WorkScheduleRepository workScheduleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AttendanceRecordRepository attendanceRepository;

    @Override
    @Transactional
    public void run(String... args) {
        seedPermissions();
        seedRoles();
        seedSalaryLevels();
        seedSystemSettings();
        seedAdminUser();
        User employee = seedEmployeeUser();
        seedWorkSchedule(employee);
        seedAttendanceRecords(employee);
    }

    private void seedAttendanceRecords(User employee) {
        if (employee.getSalaryLevel() == null || !employee.getSalaryLevel().getName().equals("Cấp 3")) {
            salaryLevelRepository.findByNameAndDeletedAtIsNull("Cấp 3").ifPresent(level -> {
                employee.setSalaryLevel(level);
                userRepository.save(employee);
            });
        }

        LocalDate today = LocalDate.now();
        Customer customer = customerRepository.findAll().get(0);

        for (int i = 1; i <= 2; i++) {
            LocalDate workDate = today.minusDays(i);
            if (!workScheduleRepository.existsByUserIdAndWorkDate(employee.getId(), workDate)) {
                WorkSchedule schedule = WorkSchedule.builder()
                        .user(employee)
                        .customer(customer)
                        .workDate(workDate)
                        .scheduledStart(LocalDateTime.of(workDate, LocalTime.of(8, 0)))
                        .scheduledEnd(LocalDateTime.of(workDate, LocalTime.of(12, 0)))
                        .status(WorkScheduleStatus.CONFIRMED)
                        .build();
                workScheduleRepository.save(schedule);

                AttendanceRecord record = AttendanceRecord.builder()
                        .workSchedule(schedule)
                        .user(employee)
                        .checkInTime(schedule.getScheduledStart())
                        .checkOutTime(schedule.getScheduledEnd())
                        .actualMinutes(240)
                        .otMultiplier(BigDecimal.valueOf(1.0))
                        .status(AttendanceStatus.ON_TIME)
                        .build();
                attendanceRepository.save(record);
            }
        }
    }

    private void seedWorkSchedule(User employee) {
        String customerPhone = "0901234567";
        Customer customer = customerRepository.findByPhoneAndDeletedAtIsNull(customerPhone)
                .orElseGet(() -> customerRepository.save(Customer.builder()
                        .name("Khách hàng Mẫu (Trung tâm Q1)")
                        .address("72 Lê Thánh Tôn, Bến Nghé, Quận 1, TP.HCM")
                        .phone(customerPhone)
                        .latitude(10.7782)
                        .longitude(106.7011)
                        .status(CustomerStatus.ACTIVE)
                        .build()));

        LocalDate today = LocalDate.now();
        if (!workScheduleRepository.existsByUserIdAndWorkDate(employee.getId(), today)) {
            WorkSchedule schedule = WorkSchedule.builder()
                    .user(employee)
                    .customer(customer)
                    .workDate(today)
                    .scheduledStart(LocalDateTime.of(today, LocalTime.of(8, 0)))
                    .scheduledEnd(LocalDateTime.of(today, LocalTime.of(12, 0)))
                    .status(WorkScheduleStatus.SCHEDULED)
                    .build();
            workScheduleRepository.save(schedule);
        }
    }

    private void seedPermissions() {
        createPermissionIfNotExists("USER_READ",    "Xem danh sách người dùng");
        createPermissionIfNotExists("USER_MANAGE",  "Quản lý người dùng");
        createPermissionIfNotExists("ROLE_READ",    "Xem danh sách vai trò");
        createPermissionIfNotExists("ROLE_MANAGE",  "Quản lý vai trò");
    }

    private void seedRoles() {
        if (!roleRepository.existsBySlug("ADMIN")) {
            Set<Permission> adminPerms = new HashSet<>(permissionRepository.findAll());
            roleRepository.save(Role.builder()
                    .slug("ADMIN")
                    .displayName("Quản trị viên")
                    .description("Toàn quyền hệ thống")
                    .isActive(true)
                    .isSystem(true)
                    .permissions(adminPerms)
                    .build());
        }
        if (!roleRepository.existsBySlug("USER")) {
            roleRepository.save(Role.builder()
                    .slug("USER")
                    .displayName("Nhân viên")
                    .description("Nhân viên phục vụ tại nhà khách hàng")
                    .isActive(true)
                    .isSystem(true)
                    .permissions(new HashSet<>())
                    .build());
        }
    }

    private void seedSalaryLevels() {
        createSalaryLevelIfNotExists("Cấp 1", 50000, "Lương cơ bản 50.000 VNĐ/giờ");
        createSalaryLevelIfNotExists("Cấp 2", 70000, "Lương cơ bản 70.000 VNĐ/giờ");
        createSalaryLevelIfNotExists("Cấp 3", 100000, "Lương cơ bản 100.000 VNĐ/giờ");
    }

    private void createSalaryLevelIfNotExists(String name, long amount, String description) {
        if (salaryLevelRepository.findByNameAndDeletedAtIsNull(name).isEmpty()) {
            salaryLevelRepository.save(SalaryLevel.builder()
                    .name(name)
                    .baseSalary(BigDecimal.valueOf(amount))
                    .description(description)
                    .isActive(true)
                    .build());
        }
    }

    private void seedSystemSettings() {
        createSettingIfNotExists("GRACE_PERIOD_MINUTES", "15", "Dung sai check-in ±15 phút (BR-11)");
        createSettingIfNotExists("TRAVEL_BUFFER_MINUTES", "15", "BR-09");
        createSettingIfNotExists("PENALTY_RULES", "[]", "BR-12");
        createSettingIfNotExists("GPS_RADIUS_METERS", "50", "BR-14");
        createSettingIfNotExists("LATE_CHECKOUT_THRESHOLD_MINUTES", "30", "BR-16.2");
    }

    private void createSettingIfNotExists(String key, String value, String description) {
        if (!systemSettingRepository.existsById(key)) {
            systemSettingRepository.save(SystemSetting.builder()
                    .key(key)
                    .value(value)
                    .description(description)
                    .build());
        }
    }

    private User seedAdminUser() {
        String adminPhone = "0900000000";
        String adminEmail = "admin@pointtrack.com";
        var existing = userRepository.findByPhoneNumberAndDeletedAtIsNull(adminPhone);
        if (existing.isPresent()) return existing.get();
        var existingByEmail = userRepository.findByEmail(adminEmail);
        if (existingByEmail.isPresent()) return existingByEmail.get();

        Role adminRole = roleRepository.findBySlug("ADMIN").orElseThrow();
        return userRepository.save(User.builder()
                .fullName("PointTrack Admin")
                .phoneNumber(adminPhone)
                .email("admin@pointtrack.com")
                .passwordHash(passwordEncoder.encode("Admin@123"))
                .status(UserStatus.ACTIVE)
                .isFirstLogin(false)
                .role(adminRole)
                .build());
    }

    private User seedEmployeeUser() {
        String employeePhone = "0123456789";
        String employeeEmail = "employee@pointtrack.com";
        var existing = userRepository.findByPhoneNumberAndDeletedAtIsNull(employeePhone);
        if (existing.isPresent()) return existing.get();
        var existingByEmail = userRepository.findByEmail(employeeEmail);
        if (existingByEmail.isPresent()) return existingByEmail.get();

        Role userRole = roleRepository.findBySlug("USER").orElseThrow();
        return userRepository.save(User.builder()
                .fullName("Employee Test")
                .phoneNumber(employeePhone)
                .email("employee@pointtrack.com")
                .passwordHash(passwordEncoder.encode("Test@1234"))
                .status(UserStatus.ACTIVE)
                .isFirstLogin(true)
                .role(userRole)
                .build());
    }

    private void createPermissionIfNotExists(String code, String name) {
        if (permissionRepository.findByCode(code).isEmpty()) {
            permissionRepository.save(Permission.builder()
                    .code(code)
                    .name(name)
                    .groupName(PermissionGroup.ADMINISTRATION)
                    .type(PermissionType.ACTION)
                    .build());
        }
    }
}
