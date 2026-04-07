package com.teco.pointtrack.controller;

import com.teco.pointtrack.dto.common.ApiResponse;
import com.teco.pointtrack.dto.packages.*;
import com.teco.pointtrack.service.ServicePackageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/packages")
@RequiredArgsConstructor
@Tag(name = "Service Package", description = "Gói dịch vụ định kỳ – tự động sinh ca theo recurrencePattern")
public class ServicePackageController {

    private final ServicePackageService packageService;

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v1/packages
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "Danh sách tất cả gói dịch vụ (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<PackageResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(packageService.getAll(), "Lấy danh sách gói thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /api/v1/packages/{id}
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết gói dịch vụ + danh sách buổi (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PackageResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(packageService.getById(id), "Lấy chi tiết gói thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/v1/packages
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping
    @Operation(
            summary = "Tạo gói dịch vụ định kỳ (ADMIN)",
            description = "Tự động sinh N ca theo recurrencePattern. " +
                          "OVERLAP → block toàn gói. BUFFER → ghi conflictDates, tiếp tục."
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PackageResponse>> create(@Valid @RequestBody PackageRequest request) {
        PackageResponse data = packageService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(data, "Tạo gói dịch vụ thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /api/v1/packages/{id}/employee — reassign nhân viên
    // ─────────────────────────────────────────────────────────────────────────

    @PutMapping("/{id}/employee")
    @Operation(
            summary = "Đổi nhân viên cho gói (chỉ các ca SCHEDULED) (ADMIN)",
            description = "Kiểm tra conflict cho nhân viên mới trước khi cập nhật"
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PackageResponse>> reassignEmployee(
            @PathVariable Long id,
            @Valid @RequestBody ReassignEmployeeRequest request) {
        PackageResponse data = packageService.reassignEmployee(id, request.getNewEmployeeId());
        return ResponseEntity.ok(ApiResponse.success(data, "Đổi nhân viên thành công"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /api/v1/packages/{id}/cancel
    // ─────────────────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}/cancel")
    @Operation(
            summary = "Huỷ gói dịch vụ (chỉ huỷ ca SCHEDULED, giữ nguyên ca COMPLETED) (ADMIN)"
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> cancel(@PathVariable Long id) {
        packageService.cancel(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Huỷ gói dịch vụ thành công"));
    }
}
