package com.teco.pointtrack.dto.packages;

import com.teco.pointtrack.entity.enums.PackageStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PackageResponse {

    Long id;

    Long customerId;
    String customerName;

    Long employeeId;
    String employeeName;

    Long templateId;
    String templateName;

    Integer totalSessions;
    Integer completedSessions;
    Integer remainingSessions;

    String recurrencePattern;
    PackageStatus status;
    String notes;

    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    // Chỉ có khi vừa tạo mới
    List<PackagePreviewResponse> previewShifts;
    List<String> conflictDates;
}
