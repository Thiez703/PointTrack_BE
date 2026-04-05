package com.teco.pointtrack.entity;

import com.teco.pointtrack.entity.enums.AttendanceStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_records", indexes = {
        @Index(name = "idx_ar_user",        columnList = "user_id"),
        @Index(name = "idx_ar_schedule",    columnList = "work_schedule_id"),
        @Index(name = "idx_ar_status",      columnList = "status"),
        @Index(name = "idx_ar_checkin",     columnList = "check_in_time")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AttendanceRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    /** 1 WorkSchedule chỉ có đúng 1 AttendanceRecord */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_schedule_id", nullable = false, unique = true)
    WorkSchedule workSchedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    // ── Check-in ─────────────────────────────────────────────────────────────

    @Column(name = "check_in_time")
    LocalDateTime checkInTime;

    @Column(name = "check_in_lat")
    Double checkInLat;

    @Column(name = "check_in_lng")
    Double checkInLng;

    /** Khoảng cách GPS thực tế tại thời điểm check-in (meters) — lưu để Admin xem */
    @Column(name = "check_in_distance_meters")
    Double checkInDistanceMeters;

    // ── Check-out ─────────────────────────────────────────────────────────────

    @Column(name = "check_out_time")
    LocalDateTime checkOutTime;

    @Column(name = "check_out_lat")
    Double checkOutLat;

    @Column(name = "check_out_lng")
    Double checkOutLng;

    @Column(name = "check_out_distance_meters")
    Double checkOutDistanceMeters;

    // ── Kết quả tổng hợp ─────────────────────────────────────────────────────

    /** Phút làm thực tế (checkOutTime - checkInTime). Null cho đến khi check-out. */
    @Column(name = "actual_minutes")
    Integer actualMinutes;

    /** Phút đi muộn so với scheduledStart + grace period */
    @Column(name = "late_minutes", nullable = false)
    @Builder.Default
    Integer lateMinutes = 0;

    /** Phút về sớm so với scheduledEnd */
    @Column(name = "early_leave_minutes", nullable = false)
    @Builder.Default
    Integer earlyLeaveMinutes = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    AttendanceStatus status = AttendanceStatus.ON_TIME;

    /**
     * Hệ số OT — snapshot từ ShiftTemplate tại thời điểm check-in.
     * BR-17: 1.0 = NORMAL, 1.5 = OT_EMERGENCY (BR-18), 2.0–3.0 = HOLIDAY
     */
    @Column(name = "ot_multiplier", nullable = false, precision = 3, scale = 1)
    @Builder.Default
    BigDecimal otMultiplier = BigDecimal.ONE;

    /** Tổng số phút làm thực tế (checkOutTime - checkInTime). Null cho đến khi check-out. */
    @Column(name = "worked_minutes")
    Long workedMinutes;

    /** Số giờ làm thực tế = workedMinutes / 60.0, làm tròn 2 chữ số thập phân. */
    @Column(name = "worked_hours")
    Double workedHours;

    /** Lương tạm tính = workedHours × ratePerHour × otMultiplier. */
    @Column(name = "estimated_salary", precision = 15, scale = 2)
    BigDecimal estimatedSalary;

    /** Ghi chú tổng hợp (Admin có thể thêm khi chỉnh sửa) */
    @Column(length = 1000)
    String note;
}
