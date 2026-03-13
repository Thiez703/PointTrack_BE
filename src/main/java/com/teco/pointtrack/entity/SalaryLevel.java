package com.teco.pointtrack.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "salary_levels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SalaryLevel extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    /** Tên cấp bậc, VD: "Junior", "Senior", "Lead" */
    @Column(nullable = false, unique = true, length = 100)
    String name;

    /** Đơn giá lương cơ bản (VNĐ) */
    @Column(name = "base_salary", nullable = false)
    BigDecimal baseSalary;

    /** Mô tả cấp bậc (nullable) */
    @Column(length = 255)
    String description;

    /** Trạng thái: true = đang dùng */
    @Builder.Default
    Boolean isActive = true;

    /** BR-22: soft delete */
    @Column(name = "deleted_at")
    LocalDateTime deletedAt;
}

