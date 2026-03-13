package com.teco.pointtrack.controller;

import com.teco.pointtrack.dto.common.ApiResponse;
import com.teco.pointtrack.dto.common.MessageResponse;
import com.teco.pointtrack.dto.personnel.EmployeePageRequest;
import com.teco.pointtrack.dto.personnel.UpdateEmployeeRequest;
import com.teco.pointtrack.dto.personnel.UpdateEmployeeStatusRequest;
import com.teco.pointtrack.dto.user.UserDetail;
import com.teco.pointtrack.service.PersonnelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Module PERSONNEL – Quản lý Nhân viên
 * Base path: /api/v1/personnel  (DC-09)
 *
 * Endpoint map:
 *   GET    /api/v1/personnel              — Danh sách nhân viên (phân trang + lọc)
 *   GET    /api/v1/personnel/{id}         — Chi tiết nhân viên
 *   PUT    /api/v1/personnel/{id}         — Cập nhật thông tin nhân viên
 *   PATCH  /api/v1/personnel/{id}/status  — Cập nhật trạng thái nhân viên
 *   DELETE /api/v1/personnel/{id}         — Xoá nhân viên (soft delete)
 */
@RestController
@RequestMapping("/api/v1/personnel")
@RequiredArgsConstructor
@Tag(name = "Personnel", description = "Quản lý Nhân viên")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class PersonnelController {

    private final PersonnelService personnelService;

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v1/personnel — Danh sách nhân viên
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Lấy danh sách nhân viên (phân trang + lọc)")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserDetail>>> getEmployees(
            @ModelAttribute EmployeePageRequest request) {

        Page<UserDetail> page = personnelService.getEmployees(request);
        return ResponseEntity.ok(
                ApiResponse.success(page, "Lấy danh sách nhân viên thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v1/personnel/{id} — Chi tiết nhân viên
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Lấy thông tin nhân viên theo ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDetail>> getEmployeeById(@PathVariable Long id) {

        UserDetail detail = personnelService.getEmployeeById(id);
        return ResponseEntity.ok(
                ApiResponse.success(detail, "Lấy thông tin nhân viên thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /api/v1/personnel/{id} — Cập nhật thông tin nhân viên
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Cập nhật thông tin nhân viên")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDetail>> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmployeeRequest request) {

        UserDetail updated = personnelService.updateEmployee(id, request);
        return ResponseEntity.ok(
                ApiResponse.success(updated, "Cập nhật thông tin nhân viên thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PATCH /api/v1/personnel/{id}/status — Cập nhật trạng thái
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Cập nhật trạng thái nhân viên")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserDetail>> updateEmployeeStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmployeeStatusRequest request) {

        UserDetail updated = personnelService.updateEmployeeStatus(id, request);
        return ResponseEntity.ok(
                ApiResponse.success(updated, "Cập nhật trạng thái nhân viên thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /api/v1/personnel/{id} — Xoá nhân viên (soft delete)
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Xoá nhân viên (soft delete)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<MessageResponse>> deleteEmployee(@PathVariable Long id) {

        personnelService.deleteEmployee(id);
        return ResponseEntity.ok(
                ApiResponse.success(
                        new MessageResponse("Xoá nhân viên thành công"),
                        "Xoá nhân viên thành công"));
    }
}

