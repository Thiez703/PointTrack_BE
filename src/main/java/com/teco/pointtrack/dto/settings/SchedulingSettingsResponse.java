package com.teco.pointtrack.dto.settings;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class SchedulingSettingsResponse {

    /** Grace period (phút) – BR-11 */
    Integer gracePeriodMinutes;

    /** Travel buffer (phút) – BR-09 */
    Integer travelBufferMinutes;

    /** Bậc thang penalty – BR-12 */
    List<PenaltyRuleItem> penaltyRules;

    LocalDateTime updatedAt;
}