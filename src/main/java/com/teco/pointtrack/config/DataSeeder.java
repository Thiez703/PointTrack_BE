package com.teco.pointtrack.config;

import com.teco.pointtrack.entity.Permission;
import com.teco.pointtrack.entity.Role;
import com.teco.pointtrack.entity.SalaryLevel;
import com.teco.pointtrack.entity.SystemSetting;
import com.teco.pointtrack.entity.User;
import com.teco.pointtrack.entity.enums.PermissionGroup;
import com.teco.pointtrack.entity.enums.PermissionType;
import com.teco.pointtrack.entity.enums.UserStatus;
import com.teco.pointtrack.repository.PermissionRepository;
import com.teco.pointtrack.repository.RoleRepository;
import com.teco.pointtrack.repository.SalaryLevelRepository;
import com.teco.pointtrack.repository.SystemSettingRepository;
import com.teco.pointtrack.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        seedPermissions();
        seedRoles();
        seedSalaryLevels();
        seedSystemSettings();
        seedAdminUser();
        seedRegularUser();
    }

    private void seedPermissions() {
        createPermissionIfNotExists("USER_READ",    "Xem danh sách người dùng", PermissionGroup.ADMINISTRATION, PermissionType.ACTION);
        createPermissionIfNotExists("USER_MANAGE",  "Quản lý người dùng",       PermissionGroup.ADMINISTRATION, PermissionType.ACTION);
        createPermissionIfNotExists("ROLE_READ",    "Xem danh sách vai trò",    PermissionGroup.ADMINISTRATION, PermissionType.ACTION);
        createPermissionIfNotExists("ROLE_MANAGE",  "Quản lý vai trò",          PermissionGroup.ADMINISTRATION, PermissionType.ACTION);
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

    private void seedAdminUser() {
        String adminPhone = "0987654321";
        if (userRepository.findByPhoneNumberAndDeletedAtIsNull(adminPhone).isEmpty()) {
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

            userRepository.save(admin);
            log.info("Seeded admin user: {} / 123456", adminPhone);
        }
    }

    private void seedRegularUser() {
        String userPhone = "0123456789";
        if (userRepository.findByPhoneNumberAndDeletedAtIsNull(userPhone).isEmpty()) {
            Role userRole = roleRepository.findBySlug("USER")
                    .orElseThrow(() -> new RuntimeException("Role USER not found"));

            User user = User.builder()
                    .fullName("PointTrack User")
                    .phoneNumber(userPhone)
                    .email("user@pointtrack.com")
                    .passwordHash(passwordEncoder.encode("123456"))
                    .status(UserStatus.ACTIVE)
                    .isFirstLogin(false)
                    .role(userRole)
                    .build();

            userRepository.save(user);
            log.info("Seeded regular user: {} / 123456", userPhone);
        }
    }

    private void createPermissionIfNotExists(String code, String name, PermissionGroup group, PermissionType type) {
        if (permissionRepository.findByCode(code).isEmpty()) {
            Permission permission = Permission.builder()
                    .code(code)
                    .name(name)
                    .groupName(group)
                    .type(type)
                    .build();
            permissionRepository.save(permission);
        }
    }
}
