package com.teco.pointtrack.controller;

import com.teco.pointtrack.common.AuthUtils;
import com.teco.pointtrack.dto.common.ApiResponse;
import com.teco.pointtrack.dto.shiftswap.*;
import com.teco.pointtrack.dto.user.UserDetail;
import com.teco.pointtrack.entity.enums.SwapStatus;
import com.teco.pointtrack.service.ShiftSwapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/shift-swap")
@RequiredArgsConstructor
@Tag(name = "Shift Swap", description = "Quản lý yêu cầu đổi ca")
public class ShiftSwapController {

    private final ShiftSwapService shiftSwapService;

    // ─────────────────────────────────────────────────────────────────────────
    // GET /v1/shift-swap
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping
    @Operation(
            summary = "Danh sách yêu cầu đổi ca",
            description = "tab=sent|received (NV). Admin bỏ qua tab để xem tất cả."
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'USER')")
    public ResponseEntity<ApiResponse<Page<ShiftSwapResponse>>> getRequests(
            @RequestParam(required = false) String tab,
            @RequestParam(required = false) SwapStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        UserDetail current = AuthUtils.getUserDetail();
        boolean isAdmin = isAdmin(current);

        Page<ShiftSwapResponse> data = shiftSwapService.getRequests(
                current.getId(), isAdmin, tab, status, page, size);

        return ResponseEntity.ok(ApiResponse.success(data, "Lấy danh sách yêu cầu đổi ca thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /v1/shift-swap/:id
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết yêu cầu đổi ca")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'USER')")
    public ResponseEntity<ApiResponse<ShiftSwapResponse>> getById(@PathVariable Long id) {
        UserDetail current = AuthUtils.getUserDetail();
        ShiftSwapResponse data = shiftSwapService.getById(id, current.getId(), isAdmin(current));
        return ResponseEntity.ok(ApiResponse.success(data, "Lấy chi tiết yêu cầu thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /v1/shift-swap — Tạo yêu cầu
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping
    @Operation(
            summary = "Tạo yêu cầu đổi ca (EMPLOYEE)",
            description = "SWAP/TRANSFER → PENDING_EMPLOYEE; SAME_DAY/OTHER_DAY → PENDING_ADMIN"
    )
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'USER')")
    public ResponseEntity<ApiResponse<ShiftSwapResponse>> create(
            @Valid @RequestBody CreateShiftSwapRequest request) {

        UserDetail current = AuthUtils.getUserDetail();
        ShiftSwapResponse data = shiftSwapService.createRequest(current.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(data, "Gửi yêu cầu đổi ca thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PATCH /v1/shift-swap/:id/respond — NV_B chấp nhận / từ chối
    // ─────────────────────────────────────────────────────────────────────────

    @PatchMapping("/{id}/respond")
    @Operation(
            summary = "NV_B phản hồi yêu cầu đổi ca",
            description = "accept=true → APPROVED + swap ngay lập tức; accept=false → REJECTED + rejectReason bắt buộc"
    )
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'USER')")
    public ResponseEntity<ApiResponse<ShiftSwapResponse>> respond(
            @PathVariable Long id,
            @RequestBody RespondSwapRequest request) {

        UserDetail current = AuthUtils.getUserDetail();
        ShiftSwapResponse data = shiftSwapService.respond(id, current.getId(), request);
        String msg = request.accept() ? "Đã chấp nhận đổi ca thành công" : "Đã từ chối yêu cầu đổi ca";
        return ResponseEntity.ok(ApiResponse.success(data, msg));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PATCH /v1/shift-swap/:id/approve — Admin duyệt
    // ─────────────────────────────────────────────────────────────────────────

    @PatchMapping("/{id}/approve")
    @Operation(summary = "Admin duyệt yêu cầu đổi ca (PENDING_ADMIN → APPROVED)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ShiftSwapResponse>> approve(@PathVariable Long id) {
        UserDetail current = AuthUtils.getUserDetail();
        ShiftSwapResponse data = shiftSwapService.approve(id, current.getId());
        return ResponseEntity.ok(ApiResponse.success(data, "Duyệt yêu cầu đổi ca thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PATCH /v1/shift-swap/:id/reject — Admin từ chối
    // ─────────────────────────────────────────────────────────────────────────

    @PatchMapping("/{id}/reject")
    @Operation(summary = "Admin từ chối yêu cầu đổi ca (lý do bắt buộc)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ShiftSwapResponse>> reject(
            @PathVariable Long id,
            @Valid @RequestBody RejectSwapRequest request) {

        UserDetail current = AuthUtils.getUserDetail();
        ShiftSwapResponse data = shiftSwapService.reject(id, current.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(data, "Đã từ chối yêu cầu đổi ca"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /v1/shift-swap/:id — NV_A hủy yêu cầu
    // ─────────────────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @Operation(summary = "NV_A hủy yêu cầu của mình (chỉ khi PENDING)")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'USER')")
    public ResponseEntity<ApiResponse<Void>> cancel(@PathVariable Long id) {
        UserDetail current = AuthUtils.getUserDetail();
        shiftSwapService.cancel(id, current.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Đã hủy yêu cầu đổi ca"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /v1/shift-swap/available-shifts
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/available-shifts")
    @Operation(
            summary = "Ca còn trống để đổi sang",
            description = "Trả các ca PUBLISHED (chưa có NV) trong ngày chỉ định"
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'USER')")
    public ResponseEntity<ApiResponse<List<ShiftSwapResponse.ShiftInfo>>> getAvailableShifts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long excludeShiftId) {

        List<ShiftSwapResponse.ShiftInfo> data =
                shiftSwapService.getAvailableShifts(date, excludeShiftId);
        return ResponseEntity.ok(ApiResponse.success(data, "Lấy danh sách ca trống thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /v1/shift-swap/available-employees
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/available-employees")
    @Operation(
            summary = "NV có ca để hoán đổi",
            description = "Trả NV đang có ca cùng ngày với ca chỉ định (để SWAP)"
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'USER')")
    public ResponseEntity<ApiResponse<List<ShiftSwapResponse.UserInfo>>> getAvailableEmployees(
            @RequestParam Long shiftId) {

        List<ShiftSwapResponse.UserInfo> data = shiftSwapService.getAvailableEmployees(shiftId);
        return ResponseEntity.ok(ApiResponse.success(data, "Lấy danh sách nhân viên thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────

    private boolean isAdmin(UserDetail user) {
        return user != null
                && user.getRole() != null
                && "ADMIN".equalsIgnoreCase(user.getRole().getSlug());
    }
}
