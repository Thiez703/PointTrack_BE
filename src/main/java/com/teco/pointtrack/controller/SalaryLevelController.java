package com.teco.pointtrack.controller;

import com.teco.pointtrack.dto.common.ApiResponse;
import com.teco.pointtrack.dto.salary.SalaryLevelRequest;
import com.teco.pointtrack.dto.salary.SalaryLevelResponse;
import com.teco.pointtrack.security.annotation.RequirePermission;
import com.teco.pointtrack.service.SalaryLevelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/salary-levels", "/v1/salary-levels"})
@RequiredArgsConstructor
@Tag(name = "Salary Level", description = "Quản lý cấp bậc lương (Cố định 3 cấp bậc)")
public class SalaryLevelController {

    private final SalaryLevelService salaryLevelService;

    @GetMapping
    @Operation(summary = "Lấy danh sách cấp bậc lương cố định")
    @RequirePermission("USER_READ")
    public ResponseEntity<ApiResponse<List<SalaryLevelResponse>>> getAll() {
        List<SalaryLevelResponse> data = salaryLevelService.getAllSalaryLevels();
        return ResponseEntity.ok(ApiResponse.success(data, "Lấy danh sách cấp bậc lương thành công"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Xem chi tiết cấp bậc lương")
    @RequirePermission("USER_READ")
    public ResponseEntity<ApiResponse<SalaryLevelResponse>> getById(@PathVariable Long id) {
        SalaryLevelResponse data = salaryLevelService.getSalaryLevelById(id);
        return ResponseEntity.ok(ApiResponse.success(data, "Lấy chi tiết cấp bậc lương thành công"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật thông tin cấp bậc lương (Số tiền/Mô tả)")
    @RequirePermission("USER_MANAGE")
    public ResponseEntity<ApiResponse<SalaryLevelResponse>> update(@PathVariable Long id, @Valid @RequestBody SalaryLevelRequest request) {
        SalaryLevelResponse data = salaryLevelService.updateSalaryLevel(id, request);
        return ResponseEntity.ok(ApiResponse.success(data, "Cập nhật cấp bậc lương thành công"));
    }
}
