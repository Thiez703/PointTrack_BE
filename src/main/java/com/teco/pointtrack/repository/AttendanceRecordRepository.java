package com.teco.pointtrack.repository;

import com.teco.pointtrack.entity.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    Optional<AttendanceRecord> findByWorkScheduleId(Long workScheduleId);

    boolean existsByWorkScheduleId(Long workScheduleId);
}
