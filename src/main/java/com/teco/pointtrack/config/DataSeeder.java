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
    private final ShiftTemplateRepository shiftTemplateRepository;
    private final WorkScheduleRepository workScheduleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        seedPermissions();
        seedRoles();
        seedSalaryLevels();
        seedSystemSettings();

        // Khởi tạo Admin nhưng không gán ca làm việc
        seedAdminUser();

        // Khởi tạo và lấy ra Employee để gán ca làm việc (Test App)
        User employee = seedEmployeeUser();
        seedWorkSchedule(employee);
    }

    private void seedWorkSchedule(User employee) {
        // 1. Tìm hoặc tạo Khách hàng (Tránh duplicate khi chạy lại code)
        String customerPhone = "0901234567";
        Customer customer = customerRepository.findByPhoneAndDeletedAtIsNull(customerPhone)
                .orElseGet(() -> {
                    Customer newCustomer = Customer.builder()
                            .name("Khách hàng Mẫu (Trung tâm Q1)")
                            .address("72 Lê Thánh Tôn, Bến Nghé, Quận 1, TP.HCM")
                            .phone(customerPhone)
                            .latitude(10.7782)
                            .longitude(106.7011)
                            .status(CustomerStatus.ACTIVE)
                            .build();
                    return customerRepository.save(newCustomer);
                });

        // 2. Tìm hoặc tạo Ca mẫu (Sáng: 08:00 - 12:00)
        String shiftName = "Ca Sáng (08:00 - 12:00)";
        ShiftTemplate shift = shiftTemplateRepository.findByNameAndDeletedAtIsNull(shiftName)
                .orElseGet(() -> {
                    ShiftTemplate newShift = ShiftTemplate.builder()
                            .name(shiftName)
                            .defaultStart(LocalTime.of(8, 0))
                            .defaultEnd(LocalTime.of(12, 0))
                            .durationMinutes(240) // FIX LỖI SQL: 4 tiếng = 240 phút
                            .shiftType(ShiftType.NORMAL)
                            .otMultiplier(BigDecimal.valueOf(1.0))
                            .isActive(true)
                            .build();
                    return shiftTemplateRepository.save(newShift);
                });

        // 3. Đảm bảo Employee này luôn có 1 ca làm việc vào NGÀY HÔM NAY để test Check-in
        LocalDate today = LocalDate.now();
        boolean hasScheduleToday = workScheduleRepository.existsByUserIdAndWorkDate(employee.getId(), today);

        if (!hasScheduleToday) {
            WorkSchedule schedule = WorkSchedule.builder()
                    .user(employee)
                    .customer(customer)
                    .shiftTemplate(shift)
                    .workDate(today)
                    .scheduledStart(LocalDateTime.of(today, shift.getDefaultStart()))
                    .scheduledEnd(LocalDateTime.of(today, shift.getDefaultEnd()))
                    .status(WorkScheduleStatus.SCHEDULED)
                    .build();
            workScheduleRepository.save(schedule);

            log.info(">>>> ĐÃ TẠO LỊCH LÀM VIỆC MẪU CHO NHÂN VIÊN ({}) VÀO HÔM NAY ({}).",
                    employee.getPhoneNumber(), today);
            log.info(">>>> Bạn có thể dùng workScheduleId = {} để test Check-in.", schedule.getId());
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
            Role adminRole = Role.builder()
                    .slug("ADMIN")
                    .displayName("Quản trị viên")
                    .description("Toàn quyền hệ thống")
                    .isActive(true)
                    .isSystem(true)
                    .permissions(adminPerms)
                    .build();
            roleRepository.save(adminRole);
            log.info("Seeded role: ADMIN");
        }

        if (!roleRepository.existsBySlug("USER")) {
            Role userRole = Role.builder()
                    .slug("USER")
                    .displayName("Nhân viên")
                    .description("Nhân viên phục vụ tại nhà khách hàng")
                    .isActive(true)
                    .isSystem(true)
                    .permissions(new HashSet<>())
                    .build();
            roleRepository.save(userRole);
            log.info("Seeded role: USER");
        }
    }

    /**
     * Tự động tạo 3 cấp bậc lương mặc định
     */
    private void seedSalaryLevels() {
        createSalaryLevelIfNotExists("Cấp 1", 50000, "Lương cơ bản 50.000 VNĐ/giờ");
        createSalaryLevelIfNotExists("Cấp 2", 70000, "Lương cơ bản 70.000 VNĐ/giờ");
        createSalaryLevelIfNotExists("Cấp 3", 100000, "Lương cơ bản 100.000 VNĐ/giờ");
    }

    private void createSalaryLevelIfNotExists(String name, long amount, String description) {
        if (salaryLevelRepository.findByNameAndDeletedAtIsNull(name).isEmpty()) {
            SalaryLevel level = SalaryLevel.builder()
                    .name(name)
                    .baseSalary(BigDecimal.valueOf(amount))
                    .description(description)
                    .isActive(true)
                    .build();
            salaryLevelRepository.save(level);
            log.info("Seeded salary level: {} ({} VNĐ/h)", name, amount);
        }
    }

    private void seedSystemSettings() {
        createSettingIfNotExists("GRACE_PERIOD_MINUTES", "5",
                "Số phút check-in muộn vẫn tính đúng giờ (BR-11)");
        createSettingIfNotExists("TRAVEL_BUFFER_MINUTES", "15",
                "Thời gian đệm di chuyển tối thiểu giữa 2 ca (BR-09)");
        createSettingIfNotExists("PENALTY_RULES",
                "[{\"minLateMinutes\":15,\"penaltyShift\":0.5},{\"minLateMinutes\":30,\"penaltyShift\":1.0}]",
                "Bậc thang trừ công khi check-in muộn (BR-12)");
        createSettingIfNotExists("GPS_RADIUS_METERS", "50",
                "Bán kính GPS fencing cho phép check-in (meters) — BR-14");
        createSettingIfNotExists("LATE_CHECKOUT_THRESHOLD_MINUTES", "30",
                "Số phút checkout quá giờ kết thúc ca bắt buộc nhập lý do — BR-16.2");
        log.info("Seeded system settings");
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
        String adminPhone = "0987654321";
        var existing = userRepository.findByPhoneNumberAndDeletedAtIsNull(adminPhone);
        if (existing.isPresent()) {
            return existing.get();
        }

        Role adminRole = roleRepository.findBySlug("ADMIN")
                .orElseThrow(() -> new RuntimeException("Role ADMIN not found"));

        User admin = User.builder()
                .fullName("PointTrack Admin")
                .phoneNumber(adminPhone)
                .email("admin@pointtrack.com")
                .passwordHash(passwordEncoder.encode("123456"))
                .status(UserStatus.ACTIVE)
                .isFirstLogin(false)
                .role(adminRole)
                .build();

        User saved = userRepository.save(admin);
        log.info("Seeded admin user: {} / 123456", adminPhone);
        return saved;
    }

    // Đổi kiểu trả về thành User để lấy dữ liệu gán vào hàm tạo Ca
    private User seedEmployeeUser() {
        String employeePhone = "0123456789";
        String tempPassword   = "Test@1234";   // satisfies BR-05: 8+ chars, uppercase, digit

        var existing = userRepository.findByPhoneNumberAndDeletedAtIsNull(employeePhone);
        if (existing.isPresent()) {
            User emp = existing.get();
            // Reset về trạng thái chưa đổi mật khẩu lần đầu để test flow first-change
            if (!emp.isFirstLogin()) {
                emp.setFirstLogin(true);
                emp.setPasswordHash(passwordEncoder.encode(tempPassword));
                userRepository.save(emp);
                log.info(">>>> RESET employee {} → isFirstLogin=true, temp password: {}", employeePhone, tempPassword);
            }
            return emp;
        }

        Role userRole = roleRepository.findBySlug("USER")
                .orElseThrow(() -> new RuntimeException("Role USER not found"));

        User employee = User.builder()
                .fullName("Employee Test")
                .phoneNumber(employeePhone)
                .email("employee@pointtrack.com")
                .passwordHash(passwordEncoder.encode(tempPassword))
                .status(UserStatus.ACTIVE)
                .isFirstLogin(true)
                .role(userRole)
                .build();

        User saved = userRepository.save(employee);
        log.info("Seeded employee user: {} / {} (isFirstLogin=true)", employeePhone, tempPassword);
        return saved;
    }

    private void createPermissionIfNotExists(String code, String name) {
        if (permissionRepository.findByCode(code).isEmpty()) {
            Permission permission = Permission.builder()
                    .code(code)
                    .name(name)
                    .groupName(PermissionGroup.ADMINISTRATION)
                    .type(PermissionType.ACTION)
                    .build();
            permissionRepository.save(permission);
        }
    }
}