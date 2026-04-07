package com.teco.pointtrack.entity;

import com.teco.pointtrack.entity.enums.SwapStatus;
import com.teco.pointtrack.entity.enums.SwapType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Yêu cầu đổi ca giữa nhân viên hoặc gửi Admin duyệt.
 */
@Entity
@Table(name = "shift_swap_requests", indexes = {
        @Index(name = "idx_swap_requester", columnList = "requester_id, status"),
        @Index(name = "idx_swap_receiver",  columnList = "receiver_id, status"),
        @Index(name = "idx_swap_status",    columnList = "status, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShiftSwapRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    // ── Loại và trạng thái ───────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,
            columnDefinition = "ENUM('SWAP','SAME_DAY','OTHER_DAY','TRANSFER')")
    SwapType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,
            columnDefinition = "ENUM('PENDING_EMPLOYEE','PENDING_ADMIN','APPROVED','REJECTED','CANCELLED')")
    @Builder.Default
    SwapStatus status = SwapStatus.PENDING_EMPLOYEE;

    // ── NV yêu cầu ──────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    User requester;

    /** Ca của NV_A muốn đổi đi */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_shift_id", nullable = false)
    Shift requesterShift;

    // ── NV nhận (null nếu gửi Admin) ────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id")
    User receiver;

    /** Ca của NV_B (SWAP: bắt buộc; TRANSFER: null) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_shift_id")
    Shift receiverShift;

    // ── Ca mục tiêu (SAME_DAY / OTHER_DAY) ──────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_shift_id")
    Shift targetShift;

    @Column(name = "target_date")
    LocalDate targetDate;

    // ── Nội dung ─────────────────────────────────────────────────────────────

    @Column(nullable = false, columnDefinition = "TEXT")
    String reason;

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    String rejectReason;

    // ── Thời hạn & Người duyệt ───────────────────────────────────────────────

    @Column(name = "expired_at")
    LocalDateTime expiredAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    User reviewedBy;

    @Column(name = "reviewed_at")
    LocalDateTime reviewedAt;
}
