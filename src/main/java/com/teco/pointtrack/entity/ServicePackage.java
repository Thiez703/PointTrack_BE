package com.teco.pointtrack.entity;

import com.teco.pointtrack.entity.enums.PackageStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

/**
 * Gói dịch vụ định kỳ: tự động sinh N ca theo recurrencePattern.
 */
@Entity
@Table(name = "service_packages", indexes = {
        @Index(name = "idx_pkg_customer",  columnList = "customer_id"),
        @Index(name = "idx_pkg_employee",  columnList = "employee_id"),
        @Index(name = "idx_pkg_status",    columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServicePackage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    // ── Relations ─────────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    User employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    ShiftTemplate template;

    // ── Sessions ──────────────────────────────────────────────────────────────

    @Column(name = "total_sessions", nullable = false)
    Integer totalSessions;

    @Column(name = "completed_sessions", nullable = false)
    @Builder.Default
    Integer completedSessions = 0;

    /**
     * JSON pattern, VD: {"days":[1,3,5],"time":"08:00"}
     * days: 1=Mon … 7=Sun (ISO weekday)
     */
    @Column(name = "recurrence_pattern", nullable = false, columnDefinition = "JSON")
    String recurrencePattern;

    @Column(columnDefinition = "TEXT")
    String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,
            columnDefinition = "ENUM('ACTIVE','COMPLETED','CANCELLED')")
    @Builder.Default
    PackageStatus status = PackageStatus.ACTIVE;
}
