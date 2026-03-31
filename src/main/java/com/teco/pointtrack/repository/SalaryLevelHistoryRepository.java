package com.teco.pointtrack.repository;

import com.teco.pointtrack.entity.SalaryLevelHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalaryLevelHistoryRepository extends JpaRepository<SalaryLevelHistory, Long> {

    /** Lấy toàn bộ lịch sử thay đổi cấp bậc của 1 nhân viên, mới nhất trước */
    @Query("""
            SELECT h FROM SalaryLevelHistory h
            LEFT JOIN FETCH h.oldLevel
            LEFT JOIN FETCH h.newLevel
            LEFT JOIN FETCH h.changedBy
            WHERE h.employee.id = :employeeId
            ORDER BY h.effectiveDate DESC, h.createdAt DESC
            """)
    List<SalaryLevelHistory> findByEmployeeIdOrderByEffectiveDateDesc(@Param("employeeId") Long employeeId);
}
