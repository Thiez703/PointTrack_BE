package com.teco.pointtrack.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.teco.pointtrack.common.StringListConverter;
import com.teco.pointtrack.entity.enums.Gender;
import com.teco.pointtrack.entity.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_deleted_at", columnList = "deleted_at"),
        @Index(name = "idx_user_status", columnList = "status")
})
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
    @Column(columnDefinition = "ENUM('ACTIVE','INACTIVE','ON_LEAVE')", nullable = false)
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

    /** BR-24: ghi nhận thời gian đổi MK gần nhất để invalidate token cũ */
    @Column(name = "password_changed_at")
    LocalDateTime passwordChangedAt;

    /** FR-02: ghi nhận thời gian đăng nhập cuối */
    @Column(name = "last_login_at")
    LocalDateTime lastLoginAt;

    /** FR-02: ghi nhận IP đăng nhập cuối */
    @Column(name = "last_login_ip", length = 45)
    String lastLoginIp;

    // ── Reset password (FR-04) ────────────────────────────────────────────────

    /** UUID token dùng để đặt lại MK, hết hạn 15 phút */
    @Column(name = "reset_password_token", length = 100)
    String resetPasswordToken;

    @Column(name = "reset_token_expired_at")
    LocalDateTime resetTokenExpiredAt;

    // ── OTP (FR-04 – SMS flow) ────────────────────────────────────────────────

    /** Mã OTP 6 chữ số gửi qua SMS */
    @Column(name = "otp_code", length = 6)
    String otpCode;

    /** Thời điểm OTP hết hạn (15 phút kể từ khi gửi) */
    @Column(name = "otp_expired_at")
    LocalDateTime otpExpiredAt;

    /** true khi NV đã xác thực OTP thành công, cho phép đặt lại MK */
    @Column(name = "otp_verified")
    Boolean otpVerified;

    // ── Soft delete (BR-22) ───────────────────────────────────────────────────

    @Column(name = "deleted_at")
    LocalDateTime deletedAt;

    /** Ngày vào làm */
    @Column(name = "start_date")
    LocalDate startDate;

    /** Khu vực phụ trách (VD: "Quận 1", "Quận Bình Thạnh") */
    @Column(name = "area", length = 100)
    String area;

    /**
     * Kỹ năng của nhân viên, lưu dạng JSON array.
     * VD: ["tam_be","ve_sinh"]
     */
    @Convert(converter = StringListConverter.class)
    @Column(name = "skills", columnDefinition = "json")
    List<String> skills;

    // ── Relations ─────────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
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
