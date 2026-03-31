package com.teco.pointtrack.controller;

import com.teco.pointtrack.common.AuthUtils;
import com.teco.pointtrack.dto.attendance.*;
import com.teco.pointtrack.dto.common.ApiResponse;
import com.teco.pointtrack.dto.common.MessageResponse;
import com.teco.pointtrack.entity.enums.AttendanceStatus;
import com.teco.pointtrack.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/v1/attendance")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Attendance", description = "Chấm công — Check-in/Check-out & Giải trình")
public class AttendanceController {

    private final AttendanceService attendanceService;

    // ─────────────────────────────────────────────────────────────────────────
    // [Admin] Danh sách bản ghi chấm công có phân trang
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(
        summary = "[Admin] Danh sách bản ghi chấm công",
        description = "Trả về danh sách bản ghi chấm công có phân trang. Tất cả filter đều optional."
    )
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/records")
    public ResponseEntity<ApiResponse<Page<AttendanceRecordResponse>>> getRecords(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) AttendanceStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<AttendanceRecordResponse> data = attendanceService.getAttendanceRecords(
                employeeId, startDate, endDate, status, page, size);
        return ResponseEntity.ok(ApiResponse.success(data, "Lấy danh sách bản ghi chấm công thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BR-14 + BR-15 + BR-16: Check-in
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(
        summary = "Check-in ca làm việc",
        description = """
            **BR-14**: Validate GPS trong bán kính `GPS_RADIUS_METERS` (mặc định 50m).
            **BR-15**: Ảnh hiện trường bắt buộc kèm metadata GPS + timestamp.
            **BR-16**: Tự động xử lý:
            - GPS sai → status = PENDING_APPROVAL, tạo ExplanationRequest(GPS_INVALID)
            - Đi muộn → status = LATE, tạo ExplanationRequest(LATE_CHECKIN)
            """
    )
    @PostMapping(value = "/check-in", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CheckInResponse>> checkIn(
            @RequestParam Long workScheduleId,
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime capturedAt,
            @RequestParam(required = false) String note,
            @RequestPart("photo") MultipartFile photo) {

        Long userId = AuthUtils.getUserDetail().getId();
        CheckInResponse result = attendanceService.checkIn(
                workScheduleId, lat, lng, capturedAt, note, photo, userId);
        return ResponseEntity.ok(ApiResponse.success(result, result.getMessage()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BR-15 + BR-16.2: Check-out
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(
        summary = "Check-out ca làm việc",
        description = """
            **BR-15**: Ảnh bắt buộc.
            **BR-16.2**: Nếu checkout quá `LATE_CHECKOUT_THRESHOLD_MINUTES` (mặc định 30 phút)
            sau giờ kết thúc ca → `checkOutReason` là BẮT BUỘC.
            """
    )
    @PostMapping(value = "/check-out", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CheckOutResponse>> checkOut(
            @RequestParam Long attendanceRecordId,
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime capturedAt,
            @RequestParam(required = false) String checkOutReason,
            @RequestPart("photo") MultipartFile photo) {

        Long userId = AuthUtils.getUserDetail().getId();
        CheckOutResponse result = attendanceService.checkOut(
                attendanceRecordId, lat, lng, capturedAt, checkOutReason, photo, userId);
        return ResponseEntity.ok(ApiResponse.success(result, result.getMessage()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Quản lý ca làm việc (Work Schedule) - Đồng bộ với giao diện FE mới
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Tạo ca làm việc mới")
    @PostMapping("/schedule/create")
    public ResponseEntity<ApiResponse<WorkScheduleResponse>> createSchedule(
            @Valid @RequestBody WorkScheduleRequest request) {
        WorkScheduleResponse data = attendanceService.createWorkSchedule(request);
        return ResponseEntity.ok(ApiResponse.success(data, "Tạo ca làm việc thành công"));
    }

    @Operation(summary = "Lấy danh sách tất cả ca làm việc")
    @GetMapping("/schedule/all")
    public ResponseEntity<ApiResponse<java.util.List<WorkScheduleResponse>>> getAllSchedules() {
        java.util.List<WorkScheduleResponse> data = attendanceService.getAllWorkSchedules();
        return ResponseEntity.ok(ApiResponse.success(data, "Lấy danh sách ca làm việc thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BR-16: Admin duyệt giải trình
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "[Admin] Duyệt đơn giải trình")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/explanations/{id}/approve")
    public ResponseEntity<MessageResponse> approveExplanation(
            @PathVariable Long id,
            @RequestBody ApprovalRequest req) {

        Long adminId = AuthUtils.getUserDetail().getId();
        attendanceService.approveExplanation(id, req, adminId);
        return ResponseEntity.ok(new MessageResponse("Đã duyệt đơn giải trình."));
    }

    @Operation(summary = "[Admin] Từ chối đơn giải trình")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/explanations/{id}/reject")
    public ResponseEntity<MessageResponse> rejectExplanation(
            @PathVariable Long id,
            @RequestBody ApprovalRequest req) {

        Long adminId = AuthUtils.getUserDetail().getId();
        attendanceService.rejectExplanation(id, req, adminId);
        return ResponseEntity.ok(new MessageResponse("Đã từ chối đơn giải trình."));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BR-19: Admin chỉnh sửa giờ công
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(
        summary = "[Admin] Chỉnh sửa giờ công",
        description = "**BR-19**: Lý do bắt buộc. Mọi thay đổi đều được ghi vào audit log."
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{recordId}/admin-update")
    public ResponseEntity<MessageResponse> adminUpdate(
            @PathVariable Long recordId,
            @Valid @RequestBody AdminUpdateAttendanceRequest req) {

        Long adminId = AuthUtils.getUserDetail().getId();
        attendanceService.adminUpdateAttendance(recordId, req, adminId);
        return ResponseEntity.ok(new MessageResponse("Cập nhật giờ công thành công. Audit log đã được ghi."));
    }
}
