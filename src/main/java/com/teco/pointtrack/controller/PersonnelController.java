package com.teco.pointtrack.controller;

import com.teco.pointtrack.dto.common.ApiResponse;
import com.teco.pointtrack.dto.personnel.AssignSalaryLevelRequest;
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

import java.util.List;

/**
 * Module PERSONNEL – Quản lý Nhân viên
 */
@RestController
@RequestMapping({"/personnel", "/v1/personnel"})
@RequiredArgsConstructor
@Tag(name = "Personnel", description = "Quản lý Nhân viên")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class PersonnelController {

    private final PersonnelService personnelService;

    @Operation(summary = "Lấy danh sách nhân viên (phân trang + lọc)")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserDetail>>> getEmployees(
            @ModelAttribute EmployeePageRequest request) {
        Page<UserDetail> page = personnelService.getEmployees(request);
        return ResponseEntity.ok(ApiResponse.success(page, "Lấy danh sách nhân viên thành công"));
    }

    @Operation(summary = "Lấy danh sách tất cả nhân viên (không phân trang - dùng cho dropdown)")
    @GetMapping("/list-all")
    public ResponseEntity<ApiResponse<List<UserDetail>>> getAllActiveEmployees() {
        List<UserDetail> list = personnelService.getAllActiveEmployees();
        return ResponseEntity.ok(ApiResponse.success(list, "Lấy danh sách nhân viên thành công"));
    }

    @Operation(summary = "Lấy thông tin nhân viên theo ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDetail>> getEmployeeById(@PathVariable Long id) {
        UserDetail detail = personnelService.getEmployeeById(id);
        return ResponseEntity.ok(ApiResponse.success(detail, "Lấy thông tin nhân viên thành công"));
    }

    @Operation(summary = "Cập nhật thông tin nhân viên (Họ tên, SĐT, Ngày sinh...)")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDetail>> updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmployeeRequest request) {
        UserDetail detail = personnelService.updateEmployee(id, request);
        return ResponseEntity.ok(ApiResponse.success(detail, "Cập nhật nhân viên thành công"));
    }

    @Operation(summary = "Gán cấp bậc lương cho nhân viên")
    @PatchMapping("/{id}/salary-level")
    public ResponseEntity<ApiResponse<UserDetail>> assignSalaryLevel(
            @PathVariable Long id,
            @Valid @RequestBody AssignSalaryLevelRequest request) {
        UserDetail detail = personnelService.assignSalaryLevel(id, request.getSalaryLevelId());
        return ResponseEntity.ok(ApiResponse.success(detail, "Gán cấp bậc lương thành công"));
    }

    @Operation(summary = "Cập nhật trạng thái nhân viên (ACTIVE/INACTIVE)")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserDetail>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmployeeStatusRequest request) {
        UserDetail detail = personnelService.updateEmployeeStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(detail, "Cập nhật trạng thái thành công"));
    }

    @Operation(summary = "Xoá nhân viên (soft delete)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEmployee(@PathVariable Long id) {
        personnelService.deleteEmployee(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Xoá nhân viên thành công"));
    }
}
