package com.teco.pointtrack.dto.attendance;

import com.teco.pointtrack.entity.enums.ExplanationStatus;
import com.teco.pointtrack.entity.enums.ExplanationType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExplanationRequestResponse {
    Long id;
    Long attendanceRecordId;
    Long userId;
    String userName;
    ExplanationType type;
    String reason;
    ExplanationStatus status;
    String reviewNote;
    String reviewedByUserName;
    LocalDateTime reviewedAt;
    LocalDateTime createdAt;
}
