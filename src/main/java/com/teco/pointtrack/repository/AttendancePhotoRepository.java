package com.teco.pointtrack.repository;

import com.teco.pointtrack.entity.AttendancePhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttendancePhotoRepository extends JpaRepository<AttendancePhoto, Long> {

    List<AttendancePhoto> findByAttendanceRecordId(Long attendanceRecordId);
}
