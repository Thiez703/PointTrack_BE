package com.teco.pointtrack.repository;

import com.teco.pointtrack.entity.AttendanceAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * BR-19: Audit log repository.
 * Chỉ expose read + save — KHÔNG có deleteById hay deleteAll được gọi từ service.
 */
public interface AttendanceAuditLogRepository extends JpaRepository<AttendanceAuditLog, Long> {

    List<AttendanceAuditLog> findByAttendanceRecordIdOrderByChangedAtDesc(Long attendanceRecordId);
}
