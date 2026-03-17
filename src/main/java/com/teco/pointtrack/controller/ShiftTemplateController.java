package com.teco.pointtrack.controller;

import com.teco.pointtrack.dto.common.ApiResponse;
import com.teco.pointtrack.dto.shift.ShiftTemplateRequest;
import com.teco.pointtrack.dto.shift.ShiftTemplateResponse;
import com.teco.pointtrack.service.ShiftTemplateService;
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
@RequestMapping("/api/v1/scheduling/shift-templates")
@RequiredArgsConstructor
@Tag(name = "Shift Template", description = "Quản lý ca mẫu (Shift Template) – CRUD ca cứng")
public class ShiftTemplateController {

    private final ShiftTemplateService shiftTemplateService;

    @GetMapping
    @Operation(summary = "Lấy danh sách ca mẫu")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ShiftTemplateResponse>>> getAll() {
        List<ShiftTemplateResponse> data = shiftTemplateService.getAll();
        return ResponseEntity.ok(ApiResponse.success(data, "Lấy danh sách ca mẫu thành công"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết ca mẫu")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ShiftTemplateResponse>> getById(@PathVariable Long id) {
        ShiftTemplateResponse data = shiftTemplateService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(data, "Lấy chi tiết ca mẫu thành công"));
    }

    @PostMapping
    @Operation(
            summary = "Tạo ca mẫu mới",
            description = "NORMAL & HOLIDAY: end phải > start (chỉ trong ngày, BR-10). " +
                          "OT_EMERGENCY: cho phép end < start (qua đêm), duration tự tính = (24h - start) + end."
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ShiftTemplateResponse>> create(@Valid @RequestBody ShiftTemplateRequest request) {
        ShiftTemplateResponse data = shiftTemplateService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(data, "Tạo ca mẫu thành công"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật ca mẫu")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ShiftTemplateResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody ShiftTemplateRequest request) {
        ShiftTemplateResponse data = shiftTemplateService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(data, "Cập nhật ca mẫu thành công"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xoá ca mẫu (soft delete)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        shiftTemplateService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Xoá ca mẫu thành công"));
    }
}