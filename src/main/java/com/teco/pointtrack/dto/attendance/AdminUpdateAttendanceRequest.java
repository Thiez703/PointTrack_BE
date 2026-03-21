package com.teco.pointtrack.dto.attendance;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * BR-19: Admin chỉnh sửa giờ công — lý do bắt buộc để ghi audit log.
 */
@Data
public class AdminUpdateAttendanceRequest {

    LocalDateTime checkInTime;
    LocalDateTime checkOutTime;

    /** Bắt buộc theo BR-19 */
    @NotBlank(message = "Lý do chỉnh sửa không được để trống")
    String reason;

    String note;
}
