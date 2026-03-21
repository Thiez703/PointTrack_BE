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
@RequestMapping({"/scheduling/settings", "/v1/scheduling/settings"})
@RequiredArgsConstructor
@Tag(name = "Scheduling Settings", description = "Cấu hình Grace Period, Penalty Rules, Travel Buffer")
public class SchedulingSettingsController {

    private final SchedulingSettingsService settingsService;

    @GetMapping
    @Operation(summary = "Xem tất cả cấu hình chấm công")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SchedulingSettingsResponse>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(
                settingsService.getAll(), "Lấy cấu hình thành công"));
    }

    @PutMapping("/grace-period")
    @Operation(
            summary = "Cập nhật Grace Period",
            description = "Số phút check-in muộn vẫn tính đúng giờ. Chỉ áp dụng check-in (BR-11). Mặc định 5 phút."
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SchedulingSettingsResponse>> updateGracePeriod(
            @Valid @RequestBody GracePeriodRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                settingsService.updateGracePeriod(request), "Cập nhật grace period thành công"));
    }

    @PutMapping("/travel-buffer")
    @Operation(
            summary = "Cập nhật Travel Buffer",
            description = "Thời gian đệm di chuyển tối thiểu giữa 2 ca liên tiếp (BR-09). Mặc định 15 phút."
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SchedulingSettingsResponse>> updateTravelBuffer(
            @Valid @RequestBody TravelBufferRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                settingsService.updateTravelBuffer(request), "Cập nhật travel buffer thành công"));
    }

    @PutMapping("/penalty-rules")
    @Operation(
            summary = "Cập nhật Penalty Rules",
            description = "Bậc thang trừ công khi check-in muộn (BR-12). " +
                          "minLateMinutes phải tăng dần. Chỉ phạt check-in muộn, KHÔNG phạt checkout sớm."
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SchedulingSettingsResponse>> updatePenaltyRules(
            @Valid @RequestBody PenaltyRulesRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                settingsService.updatePenaltyRules(request), "Cập nhật penalty rules thành công"));
    }
}
