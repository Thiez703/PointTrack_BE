package com.teco.pointtrack.controller;

import com.teco.pointtrack.dto.common.ApiResponse;
import com.teco.pointtrack.dto.employee.*;
import com.teco.pointtrack.dto.personnel.AssignSalaryLevelRequest;
import com.teco.pointtrack.dto.personnel.UpdateEmployeeStatusRequest;
import com.teco.pointtrack.entity.enums.UserStatus;
import com.teco.pointtrack.service.EmployeeImportService;
import com.teco.pointtrack.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Employee Management module – REST API.
 * Base path: /api/v1/employees  (context-path = /api)
 */
@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
@Tag(name = "Employee Management", description = "Quản lý Nhân viên – CRUD + Import Excel")
@SecurityRequirement(name = "bearerAuth")
public class EmployeeController {

    private final EmployeeService       employeeService;
    private final EmployeeImportService importService;

    // ─────────────────────────────────────────────────────────────────────────
    // ME (Profile)
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Lấy thông tin hồ sơ nhân viên đang đăng nhập (Profile/Dashboard)")
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<EmployeeProfileResponse>> getMe() {
        EmployeeProfileResponse data = employeeService.getEmployeeProfile();
        return ResponseEntity.ok(ApiResponse.success(data, "Lấy thông tin thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LIST
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Danh sách nhân viên (phân trang + lọc)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<EmployeeResponse>>> list(
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) Long salaryLevelId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<EmployeeResponse> data =
                employeeService.listEmployees(status, area, salaryLevelId, keyword, page, size);
        return ResponseEntity.ok(ApiResponse.success(data, "Lấy danh sách nhân viên thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Tạo nhân viên mới (auto-generate temp password + gửi email)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeResponse>> create(
            @Valid @RequestBody EmployeeCreateRequest request) {

        EmployeeResponse data = employeeService.createEmployee(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(data, "Tạo nhân viên thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SUMMARY STATS
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Thống kê tổng quát nhân viên (simple)",
               description = "Trả về tổng NV, đang làm, nghỉ phép, mới trong tháng. Dùng 1 DB query.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<EmployeeSummaryStats>> getSummaryStats() {
        EmployeeSummaryStats data = employeeService.getSummaryStats();
        return ResponseEntity.ok(ApiResponse.success(data, "Lấy thống kê thành công"));
    }

    @Operation(summary = "Thống kê nhân sự nâng cao (Admin dashboard)",
               description = "Trả về tổng NV, tỷ lệ ACTIVE, xu hướng so tháng trước. Dùng 1 DB query.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<EmployeeStatisticsResponse>> getStatistics() {
        EmployeeStatisticsResponse data = employeeService.getStatistics();
        return ResponseEntity.ok(ApiResponse.success(data, "Lấy thống kê nhân sự thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LIST ALL (no pagination — for dropdown)
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Danh sách tất cả NV đang ACTIVE (không phân trang, cho dropdown)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/list-all")
    public ResponseEntity<ApiResponse<List<EmployeeResponse>>> listAll() {
        List<EmployeeResponse> data = employeeService.listAllActive();
        return ResponseEntity.ok(ApiResponse.success(data, "Lấy danh sách nhân viên thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EXCEL IMPORT – template
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Tải về file Excel template để import nhân viên")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/import/template")
    public ResponseEntity<byte[]> downloadTemplate() throws IOException {
        byte[] bytes = importService.generateTemplate();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"employee_import_template.xlsx\"")
                .body(bytes);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET DETAIL
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Chi tiết nhân viên (profile + stats tháng này + lịch sử cấp bậc)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeDetailResponse>> getDetail(@PathVariable Long id) {
        EmployeeDetailResponse data = employeeService.getEmployeeDetail(id);
        return ResponseEntity.ok(ApiResponse.success(data, "Lấy thông tin nhân viên thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Cập nhật thông tin nhân viên (có auto-insert salary_level_history khi đổi cấp bậc)")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployeeResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeUpdateRequest request) {

        EmployeeResponse data = employeeService.updateEmployee(id, request);
        return ResponseEntity.ok(ApiResponse.success(data, "Cập nhật nhân viên thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SOFT DELETE
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Vô hiệu hóa nhân viên (soft delete + hủy ca tương lai)")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        String message = employeeService.deleteEmployee(id);
        return ResponseEntity.ok(ApiResponse.success(null, message));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SALARY HISTORY
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Lịch sử thay đổi cấp bậc lương của nhân viên")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}/salary-history")
    public ResponseEntity<ApiResponse<List<SalaryHistoryResponse>>> getSalaryHistory(
            @PathVariable Long id) {

        List<SalaryHistoryResponse> data = employeeService.getSalaryHistory(id);
        return ResponseEntity.ok(ApiResponse.success(data, "Lấy lịch sử cấp bậc lương thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EXCEL IMPORT
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Import danh sách nhân viên từ file Excel (.xlsx)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImportResult>> importExcel(
            @RequestParam("file") MultipartFile file) {

        ImportResult result = importService.importFromExcel(file);
        String msg = String.format("Import hoàn tất: %d thành công, %d thất bại",
                result.getSuccess(), result.getFailed());
        return ResponseEntity.ok(ApiResponse.success(result, msg));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ASSIGN SALARY LEVEL
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Gán cấp bậc lương cho nhân viên")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/salary-level")
    public ResponseEntity<ApiResponse<EmployeeResponse>> assignSalaryLevel(
            @PathVariable Long id,
            @Valid @RequestBody AssignSalaryLevelRequest request) {
        EmployeeResponse data = employeeService.assignSalaryLevel(id, request.getSalaryLevelId());
        return ResponseEntity.ok(ApiResponse.success(data, "Gán cấp bậc lương thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE STATUS
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Cập nhật trạng thái nhân viên (ACTIVE/INACTIVE/ON_LEAVE)")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<EmployeeResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmployeeStatusRequest request) {
        EmployeeUpdateRequest req = new EmployeeUpdateRequest();
        req.setStatus(request.getStatus());
        EmployeeResponse data = employeeService.updateEmployee(id, req);
        return ResponseEntity.ok(ApiResponse.success(data, "Cập nhật trạng thái thành công"));
    }
}
