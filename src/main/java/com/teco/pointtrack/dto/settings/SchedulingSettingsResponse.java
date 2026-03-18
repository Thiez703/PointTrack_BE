package com.teco.pointtrack.dto.settings;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Thông tin cấu hình lịch trình & chấm công")
public class SchedulingSettingsResponse {

    @Schema(description = "Thời gian ân hạn (số phút đi muộn tối đa vẫn tính đúng giờ)", example = "5")
    Integer gracePeriodMinutes;

    @Schema(description = "Thời gian đệm di chuyển tối thiểu giữa 2 ca (phút)", example = "15")
    Integer travelBufferMinutes;

    @Schema(description = "Danh sách các mức phạt khi đi muộn")
    List<PenaltyRuleItem> penaltyRules;

    @Schema(description = "Thời gian cập nhật gần nhất")
    LocalDateTime updatedAt;
}