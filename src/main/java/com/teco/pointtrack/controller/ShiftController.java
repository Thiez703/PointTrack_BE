package com.teco.pointtrack.controller;

import com.teco.pointtrack.common.AuthUtils;
import com.teco.pointtrack.dto.common.ApiResponse;
import com.teco.pointtrack.dto.shift.*;
import com.teco.pointtrack.dto.user.UserDetail;
import com.teco.pointtrack.entity.enums.ShiftType;
import com.teco.pointtrack.service.ShiftService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/shifts")
@RequiredArgsConstructor
@Tag(name = "Shift", description = "Quản lý ca làm việc – BR-06, BR-09, BR-10, BR-13")
public class ShiftController {

    private final ShiftService shiftService;

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v1/shifts
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping
    @Operation(
            summary = "Lấy danh sách ca làm việc",
            description = "Filter theo tuần ISO (week=2026-W12), tháng (month=3&year=2026), " +
                          "hoặc nhân viên. Kết quả nhóm theo employeeId. " +
                          "ADMIN xem tất cả; EMPLOYEE/USER chỉ xem ca của chính mình (employeeId bị bỏ qua)."
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'USER')")
    public ResponseEntity<ApiResponse<Map<String, List<ShiftResponse>>>> getShifts(
            @RequestParam(required = false) String week,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long employeeId) {

        UserDetail currentUser = AuthUtils.getUserDetail();
        boolean isAdmin = currentUser != null
                && currentUser.getRole() != null
                && "ADMIN".equalsIgnoreCase(currentUser.getRole().getSlug());

        // Nếu không phải ADMIN, bỏ qua employeeId từ request và dùng ID của chính mình
        Long effectiveEmployeeId = isAdmin ? employeeId : (currentUser != null ? currentUser.getId() : null);

        Map<String, List<ShiftResponse>> data = shiftService.getShifts(week, month, year, effectiveEmployeeId);
        return ResponseEntity.ok(ApiResponse.success(data, "Lấy danh sách ca thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v1/shifts/conflict-check
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/conflict-check")
    @Operation(
            summary = "Kiểm tra conflict trước khi lưu (BR-13)",
            description = "Trả về hasConflict=false nếu không có vấn đề. " +
                          "conflictType: OVERLAP | BUFFER | null. conflictingShiftId: ID ca gây xung đột (null nếu không có)"
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ConflictCheckResponse>> conflictCheck(
            @RequestParam Long employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate shiftDate,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime startTime,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime endTime,
            @RequestParam ShiftType shiftType,
            @RequestParam(required = false) Long excludeShiftId) {

        ConflictCheckResponse result = shiftService.preCheck(
                employeeId, shiftDate, startTime, endTime, shiftType, excludeShiftId);
        return ResponseEntity.ok(ApiResponse.success(result, "Kiểm tra conflict hoàn tất"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v1/shifts/available-employees
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/available-employees")
    @Operation(summary = "Nhân viên còn rảnh vào khung giờ chỉ định")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<AvailableEmployeeResponse>>> availableEmployees(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate shiftDate,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime startTime,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime endTime,
            @RequestParam(defaultValue = "NORMAL") ShiftType shiftType) {

        List<AvailableEmployeeResponse> result =
                shiftService.findAvailableEmployees(shiftDate, startTime, endTime, shiftType);
        return ResponseEntity.ok(ApiResponse.success(result, "Lấy danh sách nhân viên rảnh thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/v1/shifts/assign — Gán ca trực (Drag & Drop)
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/assign")
    @Operation(
            summary = "Gán nhân viên vào ca trực – Drag & Drop (ADMIN)",
            description = """
                    Tiếp nhận yêu cầu gán nhân viên vào ca trực khi kéo-thả trên lịch.

                    **Luồng xử lý:**
                    1. Validate thời gian (INVALID_TIME_RANGE)
                    2. Kiểm tra nhân viên ACTIVE (EMPLOYEE_NOT_FOUND / INACTIVE_STATUS)
                    3. Kiểm tra khách hàng ACTIVE (INACTIVE_STATUS)
                    4. Kiểm tra trùng lịch OVERLAP → 400 SCHEDULE_CONFLICT
                    5. Kiểm tra tổng giờ trong ngày ≤ 12h → 400 EXCEED_WORKING_HOURS
                    6. Kiểm tra buffer di chuyển → 201 với cảnh báo DISTANCE_WARNING nếu thiếu buffer

                    **Mã lỗi:** EMPLOYEE_NOT_FOUND | SCHEDULE_CONFLICT | INACTIVE_STATUS | INVALID_TIME_RANGE | EXCEED_WORKING_HOURS
                    """
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AssignShiftResponse>> assign(
            @Valid @RequestBody AssignShiftRequest request) {

        ShiftService.AssignShiftResult result = shiftService.assign(request);

        if (result.hasWarning()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    ApiResponse.successWithWarning(
                            result.data(),
                            "Gán ca thành công",
                            "DISTANCE_WARNING: " + result.distanceWarning()));
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(result.data(), "Gán ca thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /api/v1/shifts/{shiftId}/assign — Admin gán NV vào ca đã tồn tại
    // ─────────────────────────────────────────────────────────────────────────

    @PutMapping("/{shiftId}/assign")
    @Operation(
            summary = "Gán nhân viên vào ca đã tồn tại (ADMIN)",
            description = """
                    Chuyển trạng thái ca từ **PUBLISHED/DRAFT → ASSIGNED**.

                    **Luồng xử lý:**
                    1. Kiểm tra ca tồn tại (SHIFT_NOT_FOUND)
                    2. Kiểm tra nhân viên ACTIVE (EMPLOYEE_INACTIVE)
                    3. Kiểm tra xung đột lịch OVERLAP → 400 SCHEDULE_CONFLICT
                    4. Kiểm tra tổng giờ trong ngày ≤ 12h → 400 MAX_HOURS_EXCEEDED
                    5. Cập nhật `employee_id` và `status = ASSIGNED`

                    **Mã lỗi:** SHIFT_NOT_FOUND | EMPLOYEE_INACTIVE | SCHEDULE_CONFLICT | MAX_HOURS_EXCEEDED
                    """
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AssignToShiftResponse>> assignEmployee(
            @PathVariable Long shiftId,
            @Parameter(description = "ID nhân viên cần gán", required = true)
            @RequestParam Long employeeId) {

        AssignToShiftResponse data = shiftService.assignEmployee(shiftId, employeeId);
        return ResponseEntity.ok(ApiResponse.success(data, "Gán nhân viên thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/v1/shifts
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping
    @Operation(
            summary = "Tạo ca đơn lẻ (ADMIN)",
            description = "OT_EMERGENCY: bỏ qua buffer violation, gửi HIGH priority notification"
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ShiftResponse>> create(@Valid @RequestBody ShiftRequest request) {
        ShiftResponse data = shiftService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(data, "Tạo ca thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /api/v1/shifts/{id}
    // ─────────────────────────────────────────────────────────────────────────

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật ca làm việc (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ShiftResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody ShiftRequest request) {
        ShiftResponse data = shiftService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(data, "Cập nhật ca thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /api/v1/shifts/{id} — soft cancel
    // ─────────────────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @Operation(summary = "Huỷ ca làm việc (status → CANCELLED)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> cancel(@PathVariable Long id) {
        shiftService.cancel(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Huỷ ca thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/v1/shifts/copy-week
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/copy-week")
    @Operation(
            summary = "Copy lịch tuần sang tuần mới (ADMIN)",
            description = "Chỉ copy ca ASSIGNED/SCHEDULED/CONFIRMED. Re-validate conflict ở tuần đích. " +
                          "Response: {copied, skipped, conflicts}"
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CopyWeekResponse>> copyWeek(
            @Valid @RequestBody CopyWeekRequest request) {
        CopyWeekResponse data = shiftService.copyWeek(request);
        return ResponseEntity.ok(ApiResponse.success(data,
                "Copy tuần hoàn tất: " + data.getCopied() + " ca đã copy, " + data.getSkipped() + " ca bỏ qua"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/v1/shifts/recurring
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/recurring")
    @Operation(
            summary = "Tạo ca lặp lại theo lịch tuần (ADMIN)",
            description = "VD: NV A làm tại KH X vào Thứ 2-4-6 từ 08:00-16:00 trong 3 tháng. " +
                          "employeeId nullable → tạo ca trống (PUBLISHED). " +
                          "Tối đa 180 ngày. Response: {created, skipped, createdShiftIds, conflicts}"
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RecurringShiftResponse>> createRecurring(
            @Valid @RequestBody RecurringShiftRequest request) {
        RecurringShiftResponse data = shiftService.createRecurring(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(data,
                        "Tạo ca lặp lại hoàn tất: " + data.getCreated() + " ca đã tạo, " + data.getSkipped() + " ca bỏ qua"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v1/shifts/open
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/open")
    @Operation(
            summary = "Danh sách ca trống (PUBLISHED) – nhân viên xem để đăng ký",
            description = "Trả về các ca chưa có nhân viên, từ hôm nay trở đi."
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<List<ShiftResponse>>> getOpenShifts() {
        List<ShiftResponse> data = shiftService.getOpenShifts();
        return ResponseEntity.ok(ApiResponse.success(data, "Lấy danh sách ca trống thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/v1/shifts/{id}/claim
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/{id}/claim")
    @Operation(
            summary = "Nhân viên tự nhận ca trống (EMPLOYEE)",
            description = "Ca chuyển PUBLISHED → ASSIGNED. Kiểm tra conflict trước khi nhận."
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<ShiftResponse>> claimShift(
            @PathVariable Long id,
            @Parameter(description = "ID nhân viên nhận ca") @RequestParam Long employeeId) {
        ShiftResponse data = shiftService.claimShift(id, employeeId);
        return ResponseEntity.ok(ApiResponse.success(data, "Nhận ca thành công. Trạng thái: ASSIGNED."));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/v1/shifts/{id}/confirm
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/{id}/confirm")
    @Operation(
            summary = "Nhân viên xác nhận sẽ đi làm (EMPLOYEE)",
            description = "Ca chuyển ASSIGNED → CONFIRMED. Admin sẽ biết chắc nhân viên sẽ đến."
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'USER')")
    public ResponseEntity<ApiResponse<ShiftResponse>> confirmShift(
            @PathVariable Long id,
            @Parameter(description = "ID nhân viên xác nhận") @RequestParam Long employeeId) {
        ShiftResponse data = shiftService.confirmShift(id, employeeId);
        return ResponseEntity.ok(ApiResponse.success(data, "Xác nhận ca thành công. Trạng thái: CONFIRMED."));
    }
}
