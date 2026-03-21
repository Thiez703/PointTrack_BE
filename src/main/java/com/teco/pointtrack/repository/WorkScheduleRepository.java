package com.teco.pointtrack.repository;

import com.teco.pointtrack.entity.WorkSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WorkScheduleRepository extends JpaRepository<WorkSchedule, Long> {

    Optional<WorkSchedule> findByIdAndDeletedAtIsNull(Long id);

    /** Lấy tất cả ca của NV trong một ngày (có thể 2 ca: thường + OT) */
    @Query("""
            SELECT ws FROM WorkSchedule ws
            WHERE ws.user.id = :userId
              AND ws.workDate = :date
              AND ws.deletedAt IS NULL
            ORDER BY ws.scheduledStart ASC
            """)
    List<WorkSchedule> findByUserIdAndWorkDate(@Param("userId") Long userId,
                                               @Param("date") LocalDate date);

    boolean existsByUserIdAndWorkDate(Long userId, LocalDate date);

    /** Kiểm tra trùng lịch: cùng NV, cùng ca mẫu, cùng ngày */
    boolean existsByUserIdAndShiftTemplateIdAndWorkDateAndDeletedAtIsNull(
            Long userId, Long shiftTemplateId, LocalDate workDate);
}
