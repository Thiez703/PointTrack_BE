package com.teco.pointtrack.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.teco.pointtrack.entity.enums.Gender;
import com.teco.pointtrack.entity.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "full_name", length = 100)
    String fullName;

    /** BR-23: email duy nhất toàn hệ thống */
    @Column(unique = true, length = 150)
    String email;

    @Column(name = "password_hash", nullable = false)
    @JsonIgnore
    String passwordHash;

    @Column(name = "phone_number", length = 15, unique = true)
    String phoneNumber;

    @Column(name = "date_of_birth")
    LocalDate dateOfBirth;

    @Column(name = "avatar_url", length = 500)
    String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "TINYINT")
    Gender gender;

    /** BR-22: soft delete - không xóa vật lý */
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('ACTIVE','INACTIVE')", nullable = false)
    @Builder.Default
    UserStatus status = UserStatus.ACTIVE;

    // ── Auth fields ───────────────────────────────────────────────────────────

    /**
     * BR-02: NV mới do Admin tạo → isFirstLogin = true
     * Bắt buộc đổi MK tạm trong lần đăng nhập đầu tiên
     */
    @Column(name = "is_first_login", nullable = false)
    @Builder.Default
    boolean isFirstLogin = true;

    /** FR-02: ghi nhận thời gian đăng nhập cuối */
    @Column(name = "last_login_at")
    LocalDateTime lastLoginAt;

    /** FR-02: ghi nhận IP đăng nhập cuối */
    @Column(name = "last_login_ip", length = 45)
    String lastLoginIp;

    // ── Reset password (FR-04) ────────────────────────────────────────────────

    /** UUID token gửi qua email, hết hạn 15 phút */
    @Column(name = "reset_password_token", length = 100)
    String resetPasswordToken;

    @Column(name = "reset_token_expired_at")
    LocalDateTime resetTokenExpiredAt;

    // ── Soft delete (BR-22) ───────────────────────────────────────────────────

    @Column(name = "deleted_at")
    LocalDateTime deletedAt;

    /** Ngày vào làm */
    @Column(name = "start_date")
    LocalDate startDate;

    // ── Relations ─────────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    Role role;

    // ── Salary Level ────────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salary_level_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    SalaryLevel salaryLevel;
}
