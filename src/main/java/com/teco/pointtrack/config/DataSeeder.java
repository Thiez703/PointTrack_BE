package com.teco.pointtrack.config;

import com.teco.pointtrack.entity.Permission;
import com.teco.pointtrack.entity.Role;
import com.teco.pointtrack.entity.User;
import com.teco.pointtrack.entity.enums.PermissionGroup;
import com.teco.pointtrack.entity.enums.PermissionType;
import com.teco.pointtrack.entity.enums.UserStatus;
import com.teco.pointtrack.repository.PermissionRepository;
import com.teco.pointtrack.repository.RoleRepository;
import com.teco.pointtrack.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        seedPermissions();
        seedRoles();
        seedAdminUser();
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

    private void seedAdminUser() {
        if (!userRepository.existsByEmail("admin@pointtrack.com")) {
            Role adminRole = roleRepository.findBySlug("ADMIN")
                    .orElseThrow(() -> new RuntimeException("Role ADMIN not found"));

            User admin = User.builder()
                    .fullName("PointTrack Admin")
                    .email("admin@pointtrack.com")
                    .passwordHash(passwordEncoder.encode("Admin@123"))
                    .status(UserStatus.ACTIVE)
                    // BR-02: Admin seed sẵn → isFirstLogin = false (không cần đổi MK)
                    .isFirstLogin(false)
                    .role(adminRole)
                    .build();

            userRepository.save(admin);
            log.info("Seeded admin: admin@pointtrack.com / Admin@123");
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
