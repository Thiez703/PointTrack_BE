package com.teco.pointtrack.entity;

import com.teco.pointtrack.entity.enums.ShiftType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "shift_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShiftTemplate extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    /** Tên ca mẫu, VD: "Ca sáng 2h", "Ca tối OT" */
    @Column(nullable = false, unique = true, length = 100)
    String name;

    /** Thời lượng cố định (phút) – auto-calculated từ defaultStart/defaultEnd */
    @Column(name = "duration_minutes", nullable = false)
    Integer durationMinutes;

    /** Giờ bắt đầu mặc định */
    @Column(name = "default_start", nullable = false)
    LocalTime defaultStart;

    /** Giờ kết thúc mặc định */
    @Column(name = "default_end", nullable = false)
    LocalTime defaultEnd;

    /** Loại ca: NORMAL, HOLIDAY, OT_EMERGENCY */
    @Enumerated(EnumType.STRING)
    @Column(name = "shift_type", nullable = false)
    @Builder.Default
    ShiftType shiftType = ShiftType.NORMAL;

    /** Màu hiển thị trên lịch (hex), VD: "#4CAF50" */
    @Column(length = 7)
    @Builder.Default
    String color = "#4CAF50";

    /** Hệ số OT: 1.0 (thường), 1.5 (đột xuất), 2.0–3.0 (Lễ/Tết) */
    @Column(name = "ot_multiplier", nullable = false, precision = 3, scale = 1)
    @Builder.Default
    BigDecimal otMultiplier = BigDecimal.ONE;

    /** Trạng thái: true = đang dùng */
    @Builder.Default
    Boolean isActive = true;

    /** BR-22: soft delete */
    @Column(name = "deleted_at")
    LocalDateTime deletedAt;
}