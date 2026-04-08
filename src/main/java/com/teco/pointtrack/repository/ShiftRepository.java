package com.teco.pointtrack.repository;

import com.teco.pointtrack.entity.Shift;
import com.teco.pointtrack.entity.enums.ShiftStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShiftRepository extends JpaRepository<Shift, Long> {

    Optional<Shift> findByIdAndStatusNot(Long id, ShiftStatus status);

    // ── Conflict checking ─────────────────────────────────────────────────────

    /**
     * BR-13: Trả về các ca đang có hiệu lực của nhân viên trong khoảng ngày,
     * dùng để kiểm tra overlap & buffer (bao gồm ±1 ngày để xử lý ca qua đêm).
     * Loại trừ: CANCELLED, MISSED, MISSING_OUT (ca không xảy ra thực tế).
     */
    @Query("""
            SELECT s FROM Shift s
            WHERE s.employee.id = :employeeId
              AND s.shiftDate BETWEEN :from AND :to
              AND s.status NOT IN (
                  com.teco.pointtrack.entity.enums.ShiftStatus.CANCELLED,
                  com.teco.pointtrack.entity.enums.ShiftStatus.MISSED,
                  com.teco.pointtrack.entity.enums.ShiftStatus.MISSING_OUT
              )
              AND (:excludeId IS NULL OR s.id <> :excludeId)
            ORDER BY s.shiftDate ASC, s.startTime ASC
            """)
    List<Shift> findActiveShiftsNear(
            @Param("employeeId") Long employeeId,
            @Param("from")       LocalDate from,
            @Param("to")         LocalDate to,
            @Param("excludeId")  Long excludeId);

    // ── Calendar / schedule views ─────────────────────────────────────────────

    /** Lấy ca theo nhân viên trong khoảng ngày (dùng cho week/month view). */
    @Query("""
            SELECT s FROM Shift s
            WHERE s.shiftDate BETWEEN :from AND :to
              AND (:employeeId IS NULL OR s.employee.id = :employeeId)
              AND s.status <> com.teco.pointtrack.entity.enums.ShiftStatus.CANCELLED
            ORDER BY s.employee.id ASC, s.shiftDate ASC, s.startTime ASC
            """)
    List<Shift> findByDateRangeAndEmployee(
            @Param("from")       LocalDate from,
            @Param("to")         LocalDate to,
            @Param("employeeId") Long employeeId);

    /** Ca ASSIGNED/SCHEDULED/CONFIRMED trong khoảng ngày (dùng cho copy-week). */
    @Query("""
            SELECT s FROM Shift s
            WHERE s.shiftDate BETWEEN :from AND :to
              AND s.status IN (
                  com.teco.pointtrack.entity.enums.ShiftStatus.ASSIGNED,
                  com.teco.pointtrack.entity.enums.ShiftStatus.SCHEDULED,
                  com.teco.pointtrack.entity.enums.ShiftStatus.CONFIRMED
              )
            ORDER BY s.shiftDate ASC, s.startTime ASC
            """)
    List<Shift> findScheduledInRange(
            @Param("from") LocalDate from,
            @Param("to")   LocalDate to);

    // ── Available employees ───────────────────────────────────────────────────

    @Query("""
            SELECT DISTINCT s.employee.id FROM Shift s
            WHERE s.shiftDate = :shiftDate
              AND s.status <> com.teco.pointtrack.entity.enums.ShiftStatus.CANCELLED
            """)
    List<Long> findBusyEmployeeIdsByDate(@Param("shiftDate") LocalDate shiftDate);

    // ── Package-related ───────────────────────────────────────────────────────

    List<Shift> findAllByServicePackageIdOrderByShiftDateAsc(Long packageId);

    @Query("""
            SELECT s FROM Shift s
            WHERE s.servicePackage.id = :packageId
              AND s.status IN (
                  com.teco.pointtrack.entity.enums.ShiftStatus.ASSIGNED,
                  com.teco.pointtrack.entity.enums.ShiftStatus.SCHEDULED,
                  com.teco.pointtrack.entity.enums.ShiftStatus.CONFIRMED
              )
            """)
    List<Shift> findScheduledByPackageId(@Param("packageId") Long packageId);


    // ── Employee soft-delete cascade ──────────────────────────────────────────

    /**
     * BR-22: Lấy tất cả ca đang chờ xử lý trong tương lai của nhân viên,
     * dùng để hủy khi soft-delete employee.
     */
    @Query("""
            SELECT s FROM Shift s
            WHERE s.employee.id = :employeeId
              AND s.shiftDate >= :today
              AND s.status IN (
                  com.teco.pointtrack.entity.enums.ShiftStatus.ASSIGNED,
                  com.teco.pointtrack.entity.enums.ShiftStatus.SCHEDULED,
                  com.teco.pointtrack.entity.enums.ShiftStatus.CONFIRMED
              )
            """)
    List<Shift> findFutureScheduledByEmployee(
            @Param("employeeId") Long employeeId,
            @Param("today")      LocalDate today);

    // ── Employee stats ────────────────────────────────────────────────────────

    /** Đếm ca không bị hủy trong khoảng ngày của nhân viên */
    @Query("""
            SELECT COUNT(s) FROM Shift s
            WHERE s.employee.id = :employeeId
              AND s.shiftDate BETWEEN :from AND :to
              AND s.status <> com.teco.pointtrack.entity.enums.ShiftStatus.CANCELLED
            """)
    long countActiveByEmployeeAndDateRange(
            @Param("employeeId") Long employeeId,
            @Param("from")       LocalDate from,
            @Param("to")         LocalDate to);

    /** Đếm ca COMPLETED trong khoảng ngày của nhân viên */
    @Query("""
            SELECT COUNT(s) FROM Shift s
            WHERE s.employee.id = :employeeId
              AND s.shiftDate BETWEEN :from AND :to
              AND s.status = com.teco.pointtrack.entity.enums.ShiftStatus.COMPLETED
            """)
    long countCompletedByEmployeeAndDateRange(
            @Param("employeeId") Long employeeId,
            @Param("from")       LocalDate from,
            @Param("to")         LocalDate to);

    // ── Working-hours total (dùng cho EXCEED_WORKING_HOURS check) ────────────

    /**
     * Tổng durationMinutes của các ca active trong ngày của nhân viên.
     * Loại trừ CANCELLED, MISSED, MISSING_OUT.
     */
    @Query("""
            SELECT COALESCE(SUM(s.durationMinutes), 0) FROM Shift s
            WHERE s.employee.id = :employeeId
              AND s.shiftDate    = :shiftDate
              AND s.status NOT IN (
                  com.teco.pointtrack.entity.enums.ShiftStatus.CANCELLED,
                  com.teco.pointtrack.entity.enums.ShiftStatus.MISSED,
                  com.teco.pointtrack.entity.enums.ShiftStatus.MISSING_OUT
              )
            """)
    int sumDurationMinutesByEmployeeAndDate(
            @Param("employeeId") Long employeeId,
            @Param("shiftDate")  LocalDate shiftDate);

    // ── Open slots (ca trống) ─────────────────────────────────────────────────

    /** Tất cả ca PUBLISHED (ca trống) trong tương lai, chưa có nhân viên */
    @Query("""
            SELECT s FROM Shift s
            WHERE s.status = com.teco.pointtrack.entity.enums.ShiftStatus.PUBLISHED
              AND s.employee IS NULL
              AND s.shiftDate >= :today
            ORDER BY s.shiftDate ASC, s.startTime ASC
            """)
    List<Shift> findOpenShifts(@Param("today") LocalDate today);

    @Query("""
            SELECT s FROM Shift s
            WHERE s.status IN (
                  com.teco.pointtrack.entity.enums.ShiftStatus.ASSIGNED,
                  com.teco.pointtrack.entity.enums.ShiftStatus.SCHEDULED,
                  com.teco.pointtrack.entity.enums.ShiftStatus.CONFIRMED
              )
              AND s.shiftDate = :today
              AND s.startTime < :thresholdTime
            """)
    List<Shift> findPendingMissedShifts(
            @Param("today")         LocalDate today,
            @Param("thresholdTime") java.time.LocalTime thresholdTime);

    @Query("""
            SELECT s FROM Shift s
            WHERE s.status = com.teco.pointtrack.entity.enums.ShiftStatus.IN_PROGRESS
              AND (
                  (s.endTime > s.startTime AND s.shiftDate = :today AND s.endTime < :thresholdTime)
                  OR
                  (s.endTime < s.startTime AND s.shiftDate = :yesterday AND s.endTime < :thresholdTime)
              )
            """)
    List<Shift> findPendingMissingOutShifts(
            @Param("today")         LocalDate today,
            @Param("yesterday")     LocalDate yesterday,
            @Param("thresholdTime") java.time.LocalTime thresholdTime);

    // ── My today shifts ───────────────────────────────────────────────────────

    /**
     * Lấy các ca làm việc của nhân viên trong một khoảng ngày (thường là hôm qua -> ngày mai).
     * Dùng cho màn hình chấm công để hiển thị các ca hiện tại, ca qua đêm hoặc ca sắp tới.
     */
    @Query("""
            SELECT s FROM Shift s
            WHERE s.employee.id = :employeeId
              AND s.shiftDate BETWEEN :startDate AND :endDate
              AND s.status <> com.teco.pointtrack.entity.enums.ShiftStatus.CANCELLED
            ORDER BY s.shiftDate ASC, s.startTime ASC
            """)
    List<Shift> findRelevantShifts(
            @Param("employeeId") Long employeeId,
            @Param("startDate")  LocalDate startDate,
            @Param("endDate")    LocalDate endDate);

    @Query("""
            SELECT s FROM Shift s
            WHERE s.employee IS NOT NULL
              AND s.shiftDate BETWEEN :from AND :to
              AND s.checkInTime IS NOT NULL
              AND s.status IN (
                  com.teco.pointtrack.entity.enums.ShiftStatus.IN_PROGRESS,
                  com.teco.pointtrack.entity.enums.ShiftStatus.COMPLETED,
                  com.teco.pointtrack.entity.enums.ShiftStatus.MISSING_OUT
              )
            ORDER BY s.shiftDate ASC, s.startTime ASC
            """)
    List<Shift> findShiftsForAttendanceBackfill(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
