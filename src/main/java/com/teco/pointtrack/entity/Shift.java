package com.teco.pointtrack.entity;

import com.teco.pointtrack.entity.enums.ShiftStatus;
import com.teco.pointtrack.entity.enums.ShiftType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Ca làm việc được giao cho nhân viên tại nhà khách hàng.
 */
@Entity
@Table(name = "shifts", indexes = {
        @Index(name = "idx_shift_employee_date", columnList = "employee_id, shift_date"),
        @Index(name = "idx_shift_status",        columnList = "status"),
        @Index(name = "idx_shift_package",       columnList = "package_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Shift extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    // ── Relations ─────────────────────────────────────────────────────────────

    /** Nhân viên được phân ca (nullable khi ca ở trạng thái PUBLISHED – ca trống) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    User employee;

    /** Khách hàng được phục vụ */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    Customer customer;

    /** Ca mẫu (tuỳ chọn – nếu dùng từ template) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    ShiftTemplate template;

    /** Gói dịch vụ (tuỳ chọn – nếu ca thuộc gói) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id")
    ServicePackage servicePackage;

    // ── Thời gian ─────────────────────────────────────────────────────────────

    @Column(name = "shift_date", nullable = false)
    LocalDate shiftDate;

    @Column(name = "start_time", nullable = false)
    LocalTime startTime;

    /** BR-10: NORMAL/HOLIDAY → end > start. OT_EMERGENCY: end có thể < start (qua đêm) */
    @Column(name = "end_time", nullable = false)
    LocalTime endTime;

    /**
     * BR-06: Lương tính theo duration cố định từ template.
     * KHÔNG tính từ checkin/checkout thực tế.
     */
    @Column(name = "duration_minutes", nullable = false)
    Integer durationMinutes;

    // ── Loại ca & hệ số ──────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "shift_type", nullable = false,
            columnDefinition = "ENUM('NORMAL','HOLIDAY','OT_EMERGENCY')")
    @Builder.Default
    ShiftType shiftType = ShiftType.NORMAL;

    @Column(name = "ot_multiplier", nullable = false, precision = 3, scale = 1)
    @Builder.Default
    BigDecimal otMultiplier = BigDecimal.ONE;

    // ── Khác ──────────────────────────────────────────────────────────────────

    @Column(columnDefinition = "TEXT")
    String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,
            columnDefinition = "ENUM('DRAFT','PUBLISHED','ASSIGNED','SCHEDULED','CONFIRMED','IN_PROGRESS','COMPLETED','MISSED','MISSING_OUT','CANCELLED')")
    @Builder.Default
    ShiftStatus status = ShiftStatus.ASSIGNED;

    // ── Check-in (GPS geofence) ───────────────────────────────────────────────

    @Column(name = "check_in_time")
    java.time.LocalDateTime checkInTime;

    @Column(name = "check_in_lat")
    Double checkInLat;

    @Column(name = "check_in_lng")
    Double checkInLng;

    /** Khoảng cách Haversine tại thời điểm check-in (meters) */
    @Column(name = "check_in_distance_meters")
    Double checkInDistanceMeters;

    /** URL/path ảnh chụp hiện trường khi check-in (bắt buộc nếu ngoài geofence hoặc đi muộn) */
    @Column(name = "check_in_photo", length = 500)
    String checkInPhoto;

    // ── Check-out ─────────────────────────────────────────────────────────────

    @Column(name = "check_out_time")
    java.time.LocalDateTime checkOutTime;

    @Column(name = "check_out_lat")
    Double checkOutLat;

    @Column(name = "check_out_lng")
    Double checkOutLng;

    @Column(name = "check_out_distance_meters")
    Double checkOutDistanceMeters;

    /** Số phút thực tế làm việc = checkOutTime - checkInTime (tính khi check-out) */
    @Column(name = "actual_minutes")
    Integer actualMinutes;
}
