package com.teco.pointtrack.controller;

import com.teco.pointtrack.dto.common.ApiResponse;
import com.teco.pointtrack.dto.common.MessageResponse;
import com.teco.pointtrack.dto.salary.SalaryLevelRequest;
import com.teco.pointtrack.dto.salary.SalaryLevelResponse;
import com.teco.pointtrack.dto.user.UserDetail;
import com.teco.pointtrack.service.SalaryLevelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Module SALARY LEVEL – Quản lý Cấp bậc & Đơn giá lương
 * Base path: /api/v1/salary-levels  (DC-09)
 *
 * Endpoint map:
 *   GET    /api/v1/salary-levels                          — Danh sách cấp bậc
 *   GET    /api/v1/salary-levels/{id}                     — Chi tiết cấp bậc
 *   POST   /api/v1/salary-levels                          — Tạo cấp bậc mới (201)
 *   PUT    /api/v1/salary-levels/{id}                     — Cập nhật cấp bậc
 *   DELETE /api/v1/salary-levels/{id}                     — Xoá cấp bậc (soft delete)
 *   PATCH  /api/v1/salary-levels/{id}/assign/{employeeId} — Gán cấp bậc cho nhân viên
 */
@RestController
@RequestMapping("/api/v1/salary-levels")
@RequiredArgsConstructor
@Tag(name = "Salary Level", description = "Quản lý Cấp bậc & Đơn giá lương")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class SalaryLevelController {

    private final SalaryLevelService salaryLevelService;

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v1/salary-levels — Danh sách cấp bậc
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Lấy danh sách cấp bậc")
    @GetMapping
    public ResponseEntity<ApiResponse<List<SalaryLevelResponse>>> getAllSalaryLevels() {

        List<SalaryLevelResponse> list = salaryLevelService.getAllSalaryLevels();
        return ResponseEntity.ok(
                ApiResponse.success(list, "Lấy danh sách cấp bậc thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v1/salary-levels/{id} — Chi tiết cấp bậc
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Lấy thông tin cấp bậc theo ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SalaryLevelResponse>> getSalaryLevelById(@PathVariable Long id) {

        SalaryLevelResponse detail = salaryLevelService.getSalaryLevelById(id);
        return ResponseEntity.ok(
                ApiResponse.success(detail, "Lấy thông tin cấp bậc thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/v1/salary-levels — Tạo cấp bậc mới
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Tạo cấp bậc mới")
    @PostMapping
    public ResponseEntity<ApiResponse<SalaryLevelResponse>> createSalaryLevel(
            @Valid @RequestBody SalaryLevelRequest request) {

        SalaryLevelResponse created = salaryLevelService.createSalaryLevel(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Tạo cấp bậc thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /api/v1/salary-levels/{id} — Cập nhật cấp bậc
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Cập nhật cấp bậc")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SalaryLevelResponse>> updateSalaryLevel(
            @PathVariable Long id,
            @Valid @RequestBody SalaryLevelRequest request) {

        SalaryLevelResponse updated = salaryLevelService.updateSalaryLevel(id, request);
        return ResponseEntity.ok(
                ApiResponse.success(updated, "Cập nhật cấp bậc thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /api/v1/salary-levels/{id} — Xoá cấp bậc (soft delete)
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Xoá cấp bậc (soft delete)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<MessageResponse>> deleteSalaryLevel(@PathVariable Long id) {

        salaryLevelService.deleteSalaryLevel(id);
        return ResponseEntity.ok(
                ApiResponse.success(
                        new MessageResponse("Xoá cấp bậc thành công"),
                        "Xoá cấp bậc thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PATCH /api/v1/salary-levels/{id}/assign/{employeeId} — Gán cấp bậc cho NV
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Gán cấp bậc cho nhân viên")
    @PatchMapping("/{id}/assign/{employeeId}")
    public ResponseEntity<ApiResponse<UserDetail>> assignSalaryLevelToEmployee(
            @PathVariable Long id,
            @PathVariable Long employeeId) {

        UserDetail result = salaryLevelService.assignSalaryLevelToEmployee(employeeId, id);
        return ResponseEntity.ok(
                ApiResponse.success(result, "Gán cấp bậc cho nhân viên thành công"));
    }
}

