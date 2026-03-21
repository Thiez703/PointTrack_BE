package com.teco.pointtrack.entity;

import com.teco.pointtrack.entity.enums.ExplanationStatus;
import com.teco.pointtrack.entity.enums.ExplanationType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * BR-16: 3 flow giải trình
 *   LATE_CHECKIN  — tự động tạo khi NV đi muộn, chờ Admin duyệt
 *   LATE_CHECKOUT — NV phải nhập reason ngay tại payload check-out
 *   GPS_INVALID   — tự động tạo khi GPS nằm ngoài bán kính, vẫn cho check-in
 */
@Entity
@Table(name = "explanation_requests", indexes = {
        @Index(name = "idx_er_record",      columnList = "attendance_record_id"),
        @Index(name = "idx_er_user_status", columnList = "user_id, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExplanationRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendance_record_id", nullable = false)
    AttendanceRecord attendanceRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    ExplanationType type;

    /**
     * Lý do NV nhập:
     *   - LATE_CHECKIN:  optional (NV có thể giải thích hoặc để trống)
     *   - LATE_CHECKOUT: bắt buộc (validate ở service layer — BR-16.2)
     *   - GPS_INVALID:   optional
     */
    @Column(length = 1000)
    String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    ExplanationStatus status = ExplanationStatus.PENDING;

    // ── Admin review (BR-16 flow đơn giản: approve/reject 1 bước) ────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    User reviewedBy;

    @Column(name = "reviewed_at")
    LocalDateTime reviewedAt;

    /** Ghi chú của Admin khi duyệt/từ chối */
    @Column(name = "review_note", length = 500)
    String reviewNote;
}
