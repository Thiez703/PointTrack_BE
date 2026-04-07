package com.teco.pointtrack.repository;

import com.teco.pointtrack.entity.ShiftSwapRequest;
import com.teco.pointtrack.entity.enums.SwapStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ShiftSwapRepository extends JpaRepository<ShiftSwapRequest, Long> {

    // ── Danh sách đã gửi (requester) ─────────────────────────────────────────

    @Query("""
            SELECT r FROM ShiftSwapRequest r
            WHERE r.requester.id = :userId
              AND (:status IS NULL OR r.status = :status)
            ORDER BY r.createdAt DESC
            """)
    Page<ShiftSwapRequest> findByRequester(
            @Param("userId") Long userId,
            @Param("status") SwapStatus status,
            Pageable pageable);

    // ── Danh sách đã nhận (receiver) ─────────────────────────────────────────

    @Query("""
            SELECT r FROM ShiftSwapRequest r
            WHERE r.receiver.id = :userId
              AND (:status IS NULL OR r.status = :status)
            ORDER BY r.createdAt DESC
            """)
    Page<ShiftSwapRequest> findByReceiver(
            @Param("userId") Long userId,
            @Param("status") SwapStatus status,
            Pageable pageable);

    // ── Admin: tất cả ────────────────────────────────────────────────────────

    @Query("""
            SELECT r FROM ShiftSwapRequest r
            WHERE (:status IS NULL OR r.status = :status)
            ORDER BY r.createdAt DESC
            """)
    Page<ShiftSwapRequest> findAll(
            @Param("status") SwapStatus status,
            Pageable pageable);

    // ── Kiểm tra ca đang có yêu cầu PENDING chưa ────────────────────────────

    @Query("""
            SELECT COUNT(r) > 0 FROM ShiftSwapRequest r
            WHERE r.requesterShift.id = :shiftId
              AND r.status IN (
                  com.teco.pointtrack.entity.enums.SwapStatus.PENDING_EMPLOYEE,
                  com.teco.pointtrack.entity.enums.SwapStatus.PENDING_ADMIN
              )
            """)
    boolean existsPendingForRequesterShift(@Param("shiftId") Long shiftId);

    // ── Tự động hủy hết hạn ──────────────────────────────────────────────────

    @Query("""
            SELECT r FROM ShiftSwapRequest r
            WHERE r.status = com.teco.pointtrack.entity.enums.SwapStatus.PENDING_EMPLOYEE
              AND r.expiredAt IS NOT NULL
              AND r.expiredAt < :now
            """)
    List<ShiftSwapRequest> findExpiredPending(@Param("now") LocalDateTime now);
}
