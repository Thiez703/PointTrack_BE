package com.teco.pointtrack.controller;

import com.teco.pointtrack.common.AuthUtils;
import com.teco.pointtrack.dto.attendance.*;
import com.teco.pointtrack.dto.common.ApiResponse;
import com.teco.pointtrack.dto.common.MessageResponse;
import com.teco.pointtrack.entity.enums.AttendanceStatus;
import com.teco.pointtrack.entity.enums.ExplanationStatus;
import com.teco.pointtrack.entity.enums.ExplanationType;
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

@RestController
@RequestMapping("/attendance")
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

    @Operation(
        summary = "[Employee] Lịch sử chấm công của tôi",
        description = "Trả về danh sách bản ghi chấm công của nhân viên đang đăng nhập."
    )
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/my-records")
    public ResponseEntity<ApiResponse<Page<AttendanceRecordResponse>>> getMyRecords(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) AttendanceStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long employeeId = AuthUtils.getUserDetail().getId();
        Page<AttendanceRecordResponse> data = attendanceService.getAttendanceRecords(
                employeeId, startDate, endDate, status, page, size);
        return ResponseEntity.ok(ApiResponse.success(data, "Lấy lịch sử chấm công thành công"));
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
            @ModelAttribute @Valid CheckInRequest request,
            @RequestPart("photo") MultipartFile photo) {

        Long userId = AuthUtils.getUserDetail().getId();
        CheckInResponse result = attendanceService.checkIn(
                request.getWorkScheduleId(), request.getLatitude(), request.getLongitude(),
                request.getCapturedAt(), request.getNote(), photo, userId);
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
            @ModelAttribute @Valid CheckOutRequest request,
            @RequestPart("photo") MultipartFile photo) {

        Long userId = AuthUtils.getUserDetail().getId();
        CheckOutResponse result = attendanceService.checkOut(
                request.getAttendanceRecordId(), request.getLatitude(), request.getLongitude(),
                request.getCapturedAt(), request.getCheckOutReason(), photo, userId);
        return ResponseEntity.ok(ApiResponse.success(result, result.getMessage()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BR-16: Admin duyệt giải trình
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "[Admin] Danh sách đơn giải trình (phân trang + lọc)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/explanations")
    public ResponseEntity<ApiResponse<Page<ExplanationRequestResponse>>> listExplanations(
            @RequestParam(required = false) ExplanationStatus status,
            @RequestParam(required = false) ExplanationType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<ExplanationRequestResponse> data = attendanceService.getExplanations(status, type, page, size);
        return ResponseEntity.ok(ApiResponse.success(data, "Lấy danh sách đơn giải trình thành công"));
    }

    @Operation(summary = "[Admin] Duyệt đơn giải trình")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/explanations/{id}/approve")
    public ResponseEntity<ApiResponse<Void>> approveExplanation(
            @PathVariable Long id,
            @RequestBody ApprovalRequest req) {

        Long adminId = AuthUtils.getUserDetail().getId();
        attendanceService.approveExplanation(id, req, adminId);
        return ResponseEntity.ok(ApiResponse.success(null, "Duyệt giải trình thành công"));
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

    // ─────────────────────────────────────────────────────────────────────────
    // Admin: Lịch sử chấm công (New APIs)
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "[Admin] Danh sách lịch sử chấm công (có filter + summary)")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<AttendanceHistoryPageResponse>> getHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String shiftType) {

        // BE xử lý 0-based
        AttendanceHistoryPageResponse data = attendanceService.getAttendanceHistory(
                page - 1, limit, search, locationId, status, dateFrom, dateTo, shiftType);
        return ResponseEntity.ok(ApiResponse.success(data, "Lấy lịch sử chấm công thành công"));
    }

    @Operation(summary = "[Admin] Dropdown danh sách địa điểm")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/locations")
    public ResponseEntity<ApiResponse<java.util.List<LocationDropdownResponse>>> getLocations() {
        java.util.List<LocationDropdownResponse> data = attendanceService.getLocations();
        return ResponseEntity.ok(ApiResponse.success(data, "Lấy danh sách địa điểm thành công"));
    }

    @Operation(summary = "[Admin] Cập nhật ghi chú bản ghi chấm công")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PatchMapping("/{id}/note")
    public ResponseEntity<ApiResponse<AttendanceHistoryResponse>> updateNote(
            @PathVariable Long id,
            @Valid @RequestBody UpdateNoteRequest req) {

        AttendanceHistoryResponse result = attendanceService.updateAttendanceNote(id, req.getNote());
        return ResponseEntity.ok(ApiResponse.success(result, "Cập nhật ghi chú thành công"));
    }

    @Operation(summary = "[Admin] Xuất Excel lịch sử chấm công")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PostMapping("/export")
    public void exportToExcel(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String shiftType,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {

        attendanceService.exportAttendanceToExcel(search, locationId, status, dateFrom, dateTo, shiftType, response);
    }
}
