package com.teco.pointtrack.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * BR-19: Audit trail — ghi lại mọi chỉnh sửa giờ công bởi Admin.
 *
 * THIẾT KẾ IMMUTABLE:
 *   - Không extends BaseEntity (tránh updatedAt, updatedByUserId gây nhầm lẫn)
 *   - Service layer: không expose update/delete method
 *   - Controller layer: không có PUT/PATCH/DELETE endpoint cho resource này
 *
 * DB-LEVEL PROTECTION (chạy sau khi deploy):
 *   REVOKE DELETE, UPDATE ON pointtrack_db.attendance_audit_logs FROM 'app_user'@'%';
 *
 * THIẾT KẾ lý do chọn bảng riêng thay vì DB Trigger:
 *   1. Code Java là source of truth — dễ test, dễ review
 *   2. Trigger khó version-control và khó debug khi migrate DB
 *   3. Bảng riêng cho phép query linh hoạt (filter by field, by admin, by date)
 */
@Entity
@Table(name = "attendance_audit_logs", indexes = {
        @Index(name = "idx_aal_record",     columnList = "attendance_record_id"),
        @Index(name = "idx_aal_changed_by", columnList = "changed_by_user_id"),
        @Index(name = "idx_aal_changed_at", columnList = "changed_at")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AttendanceAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "attendance_record_id", nullable = false)
    Long attendanceRecordId;

    /** Admin thực hiện chỉnh sửa */
    @Column(name = "changed_by_user_id", nullable = false)
    Long changedByUserId;

    /** Tên field bị thay đổi, VD: "checkInTime", "status", "actualMinutes" */
    @Column(name = "field_name", nullable = false, length = 100)
    String fieldName;

    @Column(name = "old_value", length = 500)
    String oldValue;

    @Column(name = "new_value", length = 500)
    String newValue;

    /** Lý do chỉnh sửa — bắt buộc theo BR-19 */
    @Column(nullable = false, length = 1000)
    String reason;

    @Column(name = "changed_at", nullable = false, updatable = false)
    LocalDateTime changedAt;

    @PrePersist
    void prePersist() {
        if (changedAt == null) changedAt = LocalDateTime.now();
    }
}
