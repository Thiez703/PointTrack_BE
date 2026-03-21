package com.teco.pointtrack.entity;

import com.teco.pointtrack.entity.enums.WorkScheduleStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "work_schedules", indexes = {
        @Index(name = "idx_ws_user_date",   columnList = "user_id, work_date"),
        @Index(name = "idx_ws_status",      columnList = "status"),
        @Index(name = "idx_ws_deleted_at",  columnList = "deleted_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WorkSchedule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    /** NV được phân công */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    /** Ca mẫu (Optional nếu nhập tay từ FE) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_template_id")
    ShiftTemplate shiftTemplate;

    /** Địa điểm làm việc (Optional nếu nhập tay từ FE) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    Customer customer;

    /** Ngày làm việc (date-only) */
    @Column(name = "work_date", nullable = false)
    LocalDate workDate;

    /** Giờ bắt đầu (LocalTime - HH:mm:ss) */
    @Column(name = "start_time")
    java.time.LocalTime startTime;

    /** Giờ kết thúc (LocalTime - HH:mm:ss) */
    @Column(name = "end_time")
    java.time.LocalTime endTime;

    /** Địa chỉ làm việc (Nhập tay từ FE) */
    @Column(name = "address")
    String address;

    /** Tọa độ GPS (Nhập tay từ FE) */
    @Column(name = "latitude")
    Double latitude;

    @Column(name = "longitude")
    Double longitude;

    /** Thời điểm bắt đầu/kết thúc ca (LocalDateTime) - Dùng cho logic backend cũ */
    @Column(name = "scheduled_start")
    LocalDateTime scheduledStart;

    @Column(name = "scheduled_end")
    LocalDateTime scheduledEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    WorkScheduleStatus status = WorkScheduleStatus.SCHEDULED;

    @Column(length = 500)
    String note;

    /** BR-22: soft delete */
    @Column(name = "deleted_at")
    LocalDateTime deletedAt;
}
