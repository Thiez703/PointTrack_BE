package com.teco.pointtrack.repository;

import com.teco.pointtrack.entity.AttendanceRecord;
import com.teco.pointtrack.entity.enums.AttendanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
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

    /** Tính tổng số giờ OT trong khoảng thời gian (ưu tiên workedMinutes, fallback actualMinutes). */
    @Query("""
            SELECT SUM((CAST(COALESCE(a.workedMinutes, a.actualMinutes) AS double) / 60.0) * (a.otMultiplier - 1.0))
            FROM AttendanceRecord a
            WHERE a.user.id = :userId
              AND a.checkInTime >= :from
              AND a.checkInTime < :to
              AND a.otMultiplier > 1.0
              AND COALESCE(a.workedMinutes, a.actualMinutes) IS NOT NULL
            """)
    Double sumOtHoursByUserAndPeriod(
            @Param("userId") Long userId,
            @Param("from")   LocalDateTime from,
            @Param("to")     LocalDateTime to);

    /** Tính tổng số phút làm việc thực tế trong khoảng thời gian */
    @Query("""
            SELECT SUM(COALESCE(a.workedMinutes, a.actualMinutes))
            FROM AttendanceRecord a
            WHERE a.user.id = :userId
              AND a.checkInTime >= :from
              AND a.checkInTime < :to
              AND COALESCE(a.workedMinutes, a.actualMinutes) IS NOT NULL
            """)
    Long sumActualMinutesByUserAndPeriod(
            @Param("userId") Long userId,
            @Param("from")   LocalDateTime from,
            @Param("to")     LocalDateTime to);

    /** Tính tổng actualMinutes theo workDate (BR-16.2) */
    @Query("""
            SELECT SUM(COALESCE(a.workedMinutes, a.actualMinutes))
            FROM AttendanceRecord a
            JOIN a.workSchedule ws
            WHERE a.user.id = :userId
              AND ws.workDate >= :startDate
              AND ws.workDate <= :endDate
              AND COALESCE(a.workedMinutes, a.actualMinutes) IS NOT NULL
            """)
    Long sumActualMinutesByUserIdAndWorkDateBetween(
            @Param("userId") Long userId,
            @Param("startDate") java.time.LocalDate startDate,
            @Param("endDate") java.time.LocalDate endDate);

    /** Tính tổng số phút làm việc có nhân hệ số OT theo workDate (BR-16.2) */
    @Query("""
            SELECT SUM(CAST(COALESCE(a.workedMinutes, a.actualMinutes) AS double) * CAST(a.otMultiplier AS double))
            FROM AttendanceRecord a
            JOIN a.workSchedule ws
            WHERE a.user.id = :userId
              AND ws.workDate >= :startDate
              AND ws.workDate <= :endDate
              AND COALESCE(a.workedMinutes, a.actualMinutes) IS NOT NULL
            """)
    Double sumWeightedMinutesByUserIdAndWorkDateBetween(
            @Param("userId") Long userId,
            @Param("startDate") java.time.LocalDate startDate,
            @Param("endDate") java.time.LocalDate endDate);

    /** Tính tổng lương dự kiến (số giờ * hệ số OT * lương cơ bản của cấp bậc tại thời điểm đó) */
    @Query("""
            SELECT SUM(CAST(COALESCE(a.workedMinutes, a.actualMinutes) AS double) / 60.0 * a.otMultiplier * CAST(sl.baseSalary AS double))
            FROM AttendanceRecord a
            JOIN a.user u
            JOIN u.salaryLevel sl
            WHERE u.id = :userId
              AND a.checkInTime >= :from
              AND a.checkInTime < :to
              AND COALESCE(a.workedMinutes, a.actualMinutes) IS NOT NULL
            """)
    Double sumSalaryByUserAndPeriod(
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

    // ── History listing (API /attendance/history) ─────────────────────────────

    /**
     * Danh sách lịch sử chấm công với đầy đủ filter cho FE.
     *
     * <ul>
     *   <li>search  — tìm LIKE theo tên nhân viên (case-insensitive)</li>
     *   <li>customerId — lọc theo địa điểm làm việc</li>
     *   <li>status  — lọc theo trạng thái; null = tất cả</li>
     *   <li>minOtMultiplier — khi filter "overtime": chỉ lấy otMultiplier > 1.0; null = bỏ qua</li>
     *   <li>dateFrom/dateTo — lọc theo ngày làm việc (workDate)</li>
     *   <li>shiftType — "morning"/"afternoon"/"night"; null = tất cả</li>
     *   <li>morningFrom/afternoonFrom/nightFrom — ranh giới giờ ca (LocalTime cố định)</li>
     * </ul>
     *
     * <p>FETCH JOIN các to-one: user, workSchedule, customer.
     * Pagination an toàn (không có collection fetch).</p>
     */
    @Query(value = """
            SELECT a FROM AttendanceRecord a
            JOIN FETCH a.user u
            LEFT JOIN FETCH a.workSchedule ws
            LEFT JOIN FETCH ws.customer c
            WHERE (:search IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR CAST(u.id AS string) LIKE LOWER(REPLACE(:search, 'nv', '')))
              AND (:customerId IS NULL OR (c IS NOT NULL AND c.id = :customerId))
              AND (:status IS NULL OR a.status = :status)
              AND (:minOtMultiplier IS NULL OR a.otMultiplier > :minOtMultiplier)
              AND (:dateFrom IS NULL OR (ws IS NOT NULL AND ws.workDate >= :dateFrom))
              AND (:dateTo IS NULL OR (ws IS NOT NULL AND ws.workDate <= :dateTo))
              AND (:shiftType IS NULL
                   OR (:shiftType = 'morning'
                       AND ws IS NOT NULL AND ws.startTime IS NOT NULL
                       AND ws.startTime >= :morningFrom AND ws.startTime < :afternoonFrom)
                   OR (:shiftType = 'afternoon'
                       AND ws IS NOT NULL AND ws.startTime IS NOT NULL
                       AND ws.startTime >= :afternoonFrom AND ws.startTime < :nightFrom)
                   OR (:shiftType = 'night'
                       AND ws IS NOT NULL AND ws.startTime IS NOT NULL
                       AND (ws.startTime >= :nightFrom OR ws.startTime < :morningFrom)))
            ORDER BY a.id DESC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT a) FROM AttendanceRecord a
            JOIN a.user u
            LEFT JOIN a.workSchedule ws
            LEFT JOIN ws.customer c
            WHERE (:search IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR CAST(u.id AS string) LIKE LOWER(REPLACE(:search, 'nv', '')))
              AND (:customerId IS NULL OR (c IS NOT NULL AND c.id = :customerId))
              AND (:status IS NULL OR a.status = :status)
              AND (:minOtMultiplier IS NULL OR a.otMultiplier > :minOtMultiplier)
              AND (:dateFrom IS NULL OR (ws IS NOT NULL AND ws.workDate >= :dateFrom))
              AND (:dateTo IS NULL OR (ws IS NOT NULL AND ws.workDate <= :dateTo))
              AND (:shiftType IS NULL
                   OR (:shiftType = 'morning'
                       AND ws IS NOT NULL AND ws.startTime IS NOT NULL
                       AND ws.startTime >= :morningFrom AND ws.startTime < :afternoonFrom)
                   OR (:shiftType = 'afternoon'
                       AND ws IS NOT NULL AND ws.startTime IS NOT NULL
                       AND ws.startTime >= :afternoonFrom AND ws.startTime < :nightFrom)
                   OR (:shiftType = 'night'
                       AND ws IS NOT NULL AND ws.startTime IS NOT NULL
                       AND (ws.startTime >= :nightFrom OR ws.startTime < :morningFrom)))
            """)
    Page<AttendanceRecord> findHistoryByFilters(
            @Param("search")        String search,
            @Param("customerId")    Long customerId,
            @Param("status")        AttendanceStatus status,
            @Param("minOtMultiplier") BigDecimal minOtMultiplier,
            @Param("dateFrom")      LocalDate dateFrom,
            @Param("dateTo")        LocalDate dateTo,
            @Param("shiftType")     String shiftType,
            @Param("morningFrom")   LocalTime morningFrom,
            @Param("afternoonFrom") LocalTime afternoonFrom,
            @Param("nightFrom")     LocalTime nightFrom,
            Pageable pageable);

    /**
     * Đếm theo trạng thái (GROUP BY) — dùng để tính summary.
     * Không có filter status/overtime để đếm tất cả các nhóm cùng lúc.
     *
     * @return List of [AttendanceStatus, count]
     */
    @Query("""
            SELECT a.status, COUNT(a) FROM AttendanceRecord a
            JOIN a.user u
            LEFT JOIN a.workSchedule ws
            LEFT JOIN ws.customer c
            WHERE (:search IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR CAST(u.id AS string) LIKE LOWER(REPLACE(:search, 'nv', '')))
              AND (:customerId IS NULL OR (c IS NOT NULL AND c.id = :customerId))
              AND (:dateFrom IS NULL OR (ws IS NOT NULL AND ws.workDate >= :dateFrom))
              AND (:dateTo IS NULL OR (ws IS NOT NULL AND ws.workDate <= :dateTo))
              AND (:shiftType IS NULL
                   OR (:shiftType = 'morning'
                       AND ws IS NOT NULL AND ws.startTime IS NOT NULL
                       AND ws.startTime >= :morningFrom AND ws.startTime < :afternoonFrom)
                   OR (:shiftType = 'afternoon'
                       AND ws IS NOT NULL AND ws.startTime IS NOT NULL
                       AND ws.startTime >= :afternoonFrom AND ws.startTime < :nightFrom)
                   OR (:shiftType = 'night'
                       AND ws IS NOT NULL AND ws.startTime IS NOT NULL
                       AND (ws.startTime >= :nightFrom OR ws.startTime < :morningFrom)))
            GROUP BY a.status
            """)
    List<Object[]> countStatusSummaryByFilters(
            @Param("search")        String search,
            @Param("customerId")    Long customerId,
            @Param("dateFrom")      LocalDate dateFrom,
            @Param("dateTo")        LocalDate dateTo,
            @Param("shiftType")     String shiftType,
            @Param("morningFrom")   LocalTime morningFrom,
            @Param("afternoonFrom") LocalTime afternoonFrom,
            @Param("nightFrom")     LocalTime nightFrom);

    /**
     * Đếm số bản ghi có otMultiplier > 1.0 — tương ứng với "overtime" trong summary.
     */
    @Query("""
            SELECT COUNT(a) FROM AttendanceRecord a
            JOIN a.user u
            LEFT JOIN a.workSchedule ws
            LEFT JOIN ws.customer c
            WHERE a.otMultiplier > 1.0
              AND (:search IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR CAST(u.id AS string) LIKE LOWER(REPLACE(:search, 'nv', '')))
              AND (:customerId IS NULL OR (c IS NOT NULL AND c.id = :customerId))
              AND (:dateFrom IS NULL OR (ws IS NOT NULL AND ws.workDate >= :dateFrom))
              AND (:dateTo IS NULL OR (ws IS NOT NULL AND ws.workDate <= :dateTo))
              AND (:shiftType IS NULL
                   OR (:shiftType = 'morning'
                       AND ws IS NOT NULL AND ws.startTime IS NOT NULL
                       AND ws.startTime >= :morningFrom AND ws.startTime < :afternoonFrom)
                   OR (:shiftType = 'afternoon'
                       AND ws IS NOT NULL AND ws.startTime IS NOT NULL
                       AND ws.startTime >= :afternoonFrom AND ws.startTime < :nightFrom)
                   OR (:shiftType = 'night'
                       AND ws IS NOT NULL AND ws.startTime IS NOT NULL
                       AND (ws.startTime >= :nightFrom OR ws.startTime < :morningFrom)))
            """)
    long countOvertimeSummaryByFilters(
            @Param("search")        String search,
            @Param("customerId")    Long customerId,
            @Param("dateFrom")      LocalDate dateFrom,
            @Param("dateTo")        LocalDate dateTo,
            @Param("shiftType")     String shiftType,
            @Param("morningFrom")   LocalTime morningFrom,
            @Param("afternoonFrom") LocalTime afternoonFrom,
            @Param("nightFrom")     LocalTime nightFrom);

    /**
     * Lấy toàn bộ bản ghi (không phân trang) để xuất Excel.
     * Giới hạn tối đa 10,000 bản ghi — service phải kiểm tra trước khi gọi.
     */
    @Query("""
            SELECT a FROM AttendanceRecord a
            JOIN FETCH a.user u
            LEFT JOIN FETCH a.workSchedule ws
            LEFT JOIN FETCH ws.customer c
            WHERE (:search IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR CAST(u.id AS string) LIKE LOWER(REPLACE(:search, 'nv', '')))
              AND (:customerId IS NULL OR (c IS NOT NULL AND c.id = :customerId))
              AND (:status IS NULL OR a.status = :status)
              AND (:minOtMultiplier IS NULL OR a.otMultiplier > :minOtMultiplier)
              AND (:dateFrom IS NULL OR (ws IS NOT NULL AND ws.workDate >= :dateFrom))
              AND (:dateTo IS NULL OR (ws IS NOT NULL AND ws.workDate <= :dateTo))
              AND (:shiftType IS NULL
                   OR (:shiftType = 'morning'
                       AND ws IS NOT NULL AND ws.startTime IS NOT NULL
                       AND ws.startTime >= :morningFrom AND ws.startTime < :afternoonFrom)
                   OR (:shiftType = 'afternoon'
                       AND ws IS NOT NULL AND ws.startTime IS NOT NULL
                       AND ws.startTime >= :afternoonFrom AND ws.startTime < :nightFrom)
                   OR (:shiftType = 'night'
                       AND ws IS NOT NULL AND ws.startTime IS NOT NULL
                       AND (ws.startTime >= :nightFrom OR ws.startTime < :morningFrom)))
            ORDER BY a.id DESC
            """)
    List<AttendanceRecord> findHistoryForExport(
            @Param("search")        String search,
            @Param("customerId")    Long customerId,
            @Param("status")        AttendanceStatus status,
            @Param("minOtMultiplier") BigDecimal minOtMultiplier,
            @Param("dateFrom")      LocalDate dateFrom,
            @Param("dateTo")        LocalDate dateTo,
            @Param("shiftType")     String shiftType,
            @Param("morningFrom")   LocalTime morningFrom,
            @Param("afternoonFrom") LocalTime afternoonFrom,
            @Param("nightFrom")     LocalTime nightFrom,
            Pageable limitPageable);

    // ── Lịch sử số ngày làm việc (giữ nguyên) ────────────────────────────────

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
