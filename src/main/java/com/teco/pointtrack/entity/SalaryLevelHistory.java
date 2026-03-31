package com.teco.pointtrack.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * BR: Lịch sử thay đổi cấp bậc lương của nhân viên.
 * Mỗi lần Admin thay đổi salary_level_id → ghi vào bảng này.
 */
@Entity
@Table(name = "salary_level_history", indexes = {
        @Index(name = "idx_slh_employee", columnList = "employee_id"),
        @Index(name = "idx_slh_effective", columnList = "effective_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SalaryLevelHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    /** Nhân viên bị thay đổi cấp bậc */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    User employee;

    /** Cấp bậc cũ (null nếu đây là lần gán đầu tiên) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "old_level_id")
    SalaryLevel oldLevel;

    /** Cấp bậc mới */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_level_id", nullable = false)
    SalaryLevel newLevel;

    /** Ngày hiệu lực (mặc định hôm nay, Admin có thể chỉnh) */
    @Column(name = "effective_date", nullable = false)
    LocalDate effectiveDate;

    /** Admin thực hiện thay đổi */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by", nullable = false)
    User changedBy;

    /** Lý do thay đổi – bắt buộc khi Admin thay đổi cấp bậc */
    @Column(columnDefinition = "TEXT")
    String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
