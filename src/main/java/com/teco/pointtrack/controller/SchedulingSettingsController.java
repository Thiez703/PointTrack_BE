package com.teco.pointtrack.controller;

import com.teco.pointtrack.dto.common.ApiResponse;
import com.teco.pointtrack.dto.settings.*;
import com.teco.pointtrack.service.SchedulingSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/scheduling/settings")
@RequiredArgsConstructor
@Tag(name = "Cấu hình lịch trình & Chấm công", description = "Quản lý Grace Period, Quy tắc phạt đi muộn (Penalty Rules), Thời gian đệm di chuyển (Travel Buffer)")
public class SchedulingSettingsController {

    private final SchedulingSettingsService settingsService;

    @GetMapping
    @Operation(summary = "Lấy tất cả cấu hình lịch trình hiện tại")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SchedulingSettingsResponse>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(
                settingsService.getAll(), "Lấy cấu hình thành công"));
    }

    @PutMapping("/grace-period")
    @Operation(
            summary = "Cập nhật thời gian ân hạn (Grace Period)",
            description = "Số phút cho phép nhân viên đi muộn nhưng vẫn được tính đúng giờ (mặc định 5 phút). Chỉ áp dụng cho Check-in (BR-11)."
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SchedulingSettingsResponse>> updateGracePeriod(
            @Valid @RequestBody GracePeriodRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                settingsService.updateGracePeriod(request), "Cập nhật thời gian ân hạn thành công"));
    }

    @PutMapping("/travel-buffer")
    @Operation(
            summary = "Cập nhật thời gian đệm di chuyển (Travel Buffer)",
            description = "Khoảng thời gian tối thiểu giữa 2 ca làm việc liên tiếp của nhân viên (mặc định 15 phút - BR-09)."
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SchedulingSettingsResponse>> updateTravelBuffer(
            @Valid @RequestBody TravelBufferRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                settingsService.updateTravelBuffer(request), "Cập nhật thời gian đệm di chuyển thành công"));
    }

    @PutMapping("/penalty-rules")
    @Operation(
            summary = "Cập nhật quy tắc phạt đi muộn (Penalty Rules)",
            description = "Cấu hình các mức trừ công dựa trên số phút đi muộn (BR-12). Các mức phạt phải được sắp xếp theo thời gian tăng dần."
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SchedulingSettingsResponse>> updatePenaltyRules(
            @Valid @RequestBody PenaltyRulesRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                settingsService.updatePenaltyRules(request), "Cập nhật quy tắc phạt thành công"));
    }
}
