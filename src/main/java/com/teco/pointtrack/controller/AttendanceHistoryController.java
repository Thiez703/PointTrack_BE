package com.teco.pointtrack.controller;

import com.teco.pointtrack.dto.attendance.*;
import com.teco.pointtrack.dto.common.ApiResponse;
import com.teco.pointtrack.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * Controller cho API Lịch sử Chấm công.
 *
 * <p>Base path: /attendance (không có v1 prefix — khớp với FE contract)
 *
 * <p>Endpoints:
 * <ul>
 *   <li>GET  /api/attendance/history     — Danh sách + phân trang + summary</li>
 *   <li>GET  /api/attendance/locations   — Dropdown địa điểm</li>
 *   <li>PATCH /api/attendance/{id}/note  — Cập nhật ghi chú</li>
 *   <li>POST  /api/attendance/export     — Xuất Excel</li>
 * </ul>
 */
@RestController
@RequestMapping("/attendance-history")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Attendance History", description = "Lịch sử chấm công — xem, lọc, xuất Excel")
@Validated
public class AttendanceHistoryController {

    private final AttendanceService attendanceService;

    // ─────────────────────────────────────────────────────────────────────────
    // [GET] /attendance/history
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(
        summary     = "[Admin] Lịch sử chấm công",
        description = """
            Trả về danh sách bản ghi chấm công có phân trang, kèm summary theo trạng thái.

            **Filter:**
            - `search`     — tìm theo tên nhân viên (LIKE)
            - `locationId` — ID địa điểm (lấy từ /attendance/locations)
            - `status`     — on_time | late | early_leave | absent | overtime
            - `dateFrom`   — YYYY-MM-DD
            - `dateTo`     — YYYY-MM-DD
            - `shiftType`  — morning | afternoon | night
            """
    )
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<AttendanceHistoryPageResponse>> getHistory(
            @RequestParam(defaultValue = "1")  @Min(value = 1, message = "page phải >= 1") int page,
            @RequestParam(defaultValue = "20") @Min(10) @Max(100) int limit,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false)
            @Pattern(regexp = "^(on_time|late|early_leave|absent|overtime|)$",
                     message = "status không hợp lệ")
            String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false)
            @Pattern(regexp = "^(morning|afternoon|night|)$",
                     message = "shiftType không hợp lệ")
            String shiftType) {

        // FE gửi 1-indexed, Spring Data JPA dùng 0-indexed
        AttendanceHistoryPageResponse data = attendanceService.getAttendanceHistory(
                page - 1, limit, search, locationId, status, dateFrom, dateTo, shiftType);

        return ResponseEntity.ok(ApiResponse.success(data, "Lấy lịch sử chấm công thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // [GET] /attendance/locations
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(
        summary     = "Danh sách địa điểm",
        description = "Trả về danh sách địa điểm để FE render dropdown filter lịch sử."
    )
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/locations")
    public ResponseEntity<ApiResponse<List<LocationDropdownResponse>>> getLocations() {
        List<LocationDropdownResponse> data = attendanceService.getLocations();
        return ResponseEntity.ok(ApiResponse.success(data, "Lấy danh sách địa điểm thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // [PATCH] /attendance/{id}/note
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(
        summary     = "[Admin] Cập nhật ghi chú bản ghi chấm công",
        description = "Cập nhật trường note. Trả về bản ghi đã được cập nhật."
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/note")
    public ResponseEntity<ApiResponse<AttendanceHistoryResponse>> updateNote(
            @PathVariable Long id,
            @Valid @RequestBody UpdateNoteRequest request) {

        AttendanceHistoryResponse updated = attendanceService.updateAttendanceNote(id, request.getNote());
        return ResponseEntity.ok(ApiResponse.success(updated, "Cập nhật ghi chú thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // [POST] /attendance/export
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(
        summary     = "[Admin] Xuất Excel lịch sử chấm công",
        description = """
            Xuất file Excel với cùng các filter như GET /history (không có page/limit).
            Giới hạn tối đa **10,000 bản ghi** — trả 400 nếu kết quả vượt ngưỡng.

            File trả về: `cham-cong-YYYYMMDD.xlsx`
            """
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/export")
    public void exportExcel(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false)
            @Pattern(regexp = "^(on_time|late|early_leave|absent|overtime|)$",
                     message = "status không hợp lệ")
            String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false)
            @Pattern(regexp = "^(morning|afternoon|night|)$",
                     message = "shiftType không hợp lệ")
            String shiftType,
            HttpServletResponse response) throws IOException {

        attendanceService.exportAttendanceToExcel(
                search, locationId, status, dateFrom, dateTo, shiftType, response);
    }
}
