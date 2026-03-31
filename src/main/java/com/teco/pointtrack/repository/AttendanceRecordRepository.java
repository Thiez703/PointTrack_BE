package com.teco.pointtrack.repository;

import com.teco.pointtrack.entity.AttendanceRecord;
import com.teco.pointtrack.entity.enums.AttendanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    Optional<AttendanceRecord> findByWorkScheduleId(Long workScheduleId);

    boolean existsByWorkScheduleId(Long workScheduleId);

    // ── Employee stats ────────────────────────────────────────────────────────

    /** Đếm số lần check-in muộn (lateMinutes > 0) trong khoảng thời gian */
    @Query("""
            SELECT COUNT(a) FROM AttendanceRecord a
            WHERE a.user.id = :userId
              AND a.checkInTime >= :from
              AND a.checkInTime < :to
              AND a.lateMinutes > 0
            """)
    long countLateCheckinsByUserAndPeriod(
            @Param("userId") Long userId,
            @Param("from")   LocalDateTime from,
            @Param("to")     LocalDateTime to);

    /** Đếm số lần về sớm (earlyLeaveMinutes > 0) trong khoảng thời gian */
    @Query("""
            SELECT COUNT(a) FROM AttendanceRecord a
            WHERE a.user.id = :userId
              AND a.checkInTime >= :from
              AND a.checkInTime < :to
              AND a.earlyLeaveMinutes > 0
            """)
    long countEarlyLeavesByUserAndPeriod(
            @Param("userId") Long userId,
            @Param("from")   LocalDateTime from,
            @Param("to")     LocalDateTime to);

    /** Đếm số ngày làm việc thực tế (unique days) trong khoảng thời gian */
    @Query("""
            SELECT COUNT(DISTINCT FUNCTION('DATE', a.checkInTime))
            FROM AttendanceRecord a
            WHERE a.user.id = :userId
              AND a.checkInTime >= :from
              AND a.checkInTime < :to
              AND a.checkInTime IS NOT NULL
            """)
    long countWorkDaysByUserAndPeriod(
            @Param("userId") Long userId,
            @Param("from")   LocalDateTime from,
            @Param("to")     LocalDateTime to);

    /** Tính tổng số giờ OT trong khoảng thời gian (dựa trên actualMinutes và otMultiplier) */
    @Query("""
            SELECT SUM((CAST(a.actualMinutes AS double) / 60.0) * (a.otMultiplier - 1.0))
            FROM AttendanceRecord a
            WHERE a.user.id = :userId
              AND a.checkInTime >= :from
              AND a.checkInTime < :to
              AND a.otMultiplier > 1.0
              AND a.actualMinutes IS NOT NULL
            """)
    Double sumOtHoursByUserAndPeriod(
            @Param("userId") Long userId,
            @Param("from")   LocalDateTime from,
            @Param("to")     LocalDateTime to);

    // ── Admin records listing ─────────────────────────────────────────────────

    @Query("""
            SELECT a FROM AttendanceRecord a
            WHERE (:employeeId IS NULL OR a.user.id = :employeeId)
              AND (:startDate  IS NULL OR a.checkInTime >= :startDate)
              AND (:endDate    IS NULL OR a.checkInTime < :endDate)
              AND (:status     IS NULL OR a.status = :status)
            ORDER BY a.checkInTime DESC
            """)
    Page<AttendanceRecord> findByFilters(
            @Param("employeeId") Long employeeId,
            @Param("startDate")  LocalDateTime startDate,
            @Param("endDate")    LocalDateTime endDate,
            @Param("status")     AttendanceStatus status,
            Pageable pageable);

    /** Lấy lịch sử số ngày làm việc trong 6 tháng gần nhất */
    @Query("""
            SELECT MONTH(a.checkInTime) as m, COUNT(DISTINCT FUNCTION('DATE', a.checkInTime)) as d
            FROM AttendanceRecord a
            WHERE a.user.id = :userId
              AND a.checkInTime >= :from
            GROUP BY MONTH(a.checkInTime)
            ORDER BY MIN(a.checkInTime) ASC
            """)
    java.util.List<Object[]> countWorkDaysHistoryByUser(
            @Param("userId") Long userId,
            @Param("from")   LocalDateTime from);
}
