package com.teco.pointtrack.repository;

import com.teco.pointtrack.entity.ExplanationRequest;
import com.teco.pointtrack.entity.enums.ExplanationStatus;
import com.teco.pointtrack.entity.enums.ExplanationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExplanationRequestRepository extends JpaRepository<ExplanationRequest, Long> {

    List<ExplanationRequest> findByAttendanceRecordId(Long attendanceRecordId);

    /** Admin: xem danh sách giải trình theo status (phân trang) */
    Page<ExplanationRequest> findByStatus(ExplanationStatus status, Pageable pageable);

    /** Admin: xem danh sách giải trình theo type (phân trang) */
    Page<ExplanationRequest> findByType(ExplanationType type, Pageable pageable);

    /** Admin: xem danh sách giải trình lọc theo cả status và type (phân trang) */
    Page<ExplanationRequest> findByStatusAndType(ExplanationStatus status, ExplanationType type, Pageable pageable);
}
