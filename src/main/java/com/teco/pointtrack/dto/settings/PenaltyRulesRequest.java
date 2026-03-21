package com.teco.pointtrack.dto.settings;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PenaltyRulesRequest {

    /**
     * Danh sách bậc thang penalty, phải sắp xếp tăng dần theo minLateMinutes.
     * VD: [{minLateMinutes:15, penaltyShift:0.5}, {minLateMinutes:30, penaltyShift:1.0}]
     */
    @NotEmpty(message = "Penalty rules không được rỗng")
    @Valid
    List<PenaltyRuleItem> rules;
}