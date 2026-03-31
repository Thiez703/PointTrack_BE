package com.teco.pointtrack.entity;

import com.teco.pointtrack.entity.enums.CustomerSource;
import com.teco.pointtrack.entity.enums.CustomerStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Thực thể Khách hàng.
 *
 * <p>Soft-delete: Khi Admin xóa KH → set {@code status = INACTIVE} và {@code deletedAt = NOW()}.
 * Không bao giờ xóa vật lý khỏi DB (BR-22).
 */
@Entity
@Table(name = "customers", indexes = {
        @Index(name = "idx_customer_status",       columnList = "status"),
        @Index(name = "idx_customer_phone",        columnList = "phone"),
        @Index(name = "idx_customer_deleted_at",   columnList = "deleted_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Customer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    // ── Thông tin cơ bản ──────────────────────────────────────────────────────

    /** Tên khách hàng */
    @Column(nullable = false, length = 255)
    String name;

    /** SĐT chính (10 chữ số, bắt đầu 0) */
    @Column(length = 20)
    String phone;

    /** SĐT phụ (tuỳ chọn) */
    @Column(name = "secondary_phone", length = 20)
    String secondaryPhone;

    // ── Địa chỉ ──────────────────────────────────────────────────────────────

    /**
     * Địa chỉ dạng chuỗi đầy đủ (VD: "123 Nguyễn Văn A, Phường 1, Quận 1, TP.HCM").
     * Dùng cho geocoding và hiển thị.
     */
    @Column(columnDefinition = "TEXT")
    String address;

    // ── GPS Coordinates (BR-14) ───────────────────────────────────────────────

    /** Vĩ độ — null nếu geocoding chưa thành công */
    @Column
    Double latitude;

    /** Kinh độ — null nếu geocoding chưa thành công */
    @Column
    Double longitude;

    // ── Ghi chú & ưu tiên ────────────────────────────────────────────────────

    /** Ghi chú đặc biệt (dị ứng, thú cưng, khóa cửa…) */
    @Column(name = "special_notes", columnDefinition = "TEXT")
    String specialNotes;

    /** Khung giờ / thời điểm KH ưa thích được phục vụ */
    @Column(name = "preferred_time_note", length = 255)
    String preferredTimeNote;

    // ── Nguồn KH ─────────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('ZALO','FACEBOOK','REFERRAL','HOTLINE','OTHER')",
            nullable = false)
    @Builder.Default
    CustomerSource source = CustomerSource.OTHER;

    // ── Trạng thái ───────────────────────────────────────────────────────────

    /**
     * Trạng thái KH (BR-14).
     * ACTIVE   → có thể gán ca mới.
     * INACTIVE → không gán ca mới (do Admin vô hiệu hóa).
     * SUSPENDED → tạm ngưng (chờ Admin kích hoạt lại).
     */
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "ENUM('ACTIVE','INACTIVE','SUSPENDED')",
            nullable = false)
    @Builder.Default
    CustomerStatus status = CustomerStatus.ACTIVE;

    // ── Soft delete (BR-22) ───────────────────────────────────────────────────

    /** Thời điểm bị xóa mềm; null = chưa xóa */
    @Column(name = "deleted_at")
    LocalDateTime deletedAt;

}
