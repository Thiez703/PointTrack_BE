package com.teco.pointtrack.dto.settings;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Một mốc trong bậc thang penalty.
 * VD: muộn > 15p → trừ 0.5 công; muộn > 30p → trừ 1.0 công
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PenaltyRuleItem {

    /** Số phút muộn tối thiểu để áp mức phạt này (sau grace period) */
    @NotNull
    @Min(value = 1, message = "minLateMinutes phải >= 1")
    Integer minLateMinutes;

    /** Số công bị trừ. VD: 0.5 = trừ nửa công */
    @NotNull
    @DecimalMin(value = "0.1", message = "penaltyShift phải >= 0.1")
    Double penaltyShift;
}