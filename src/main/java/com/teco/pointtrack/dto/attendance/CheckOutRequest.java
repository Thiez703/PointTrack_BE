package com.teco.pointtrack.dto.attendance;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class CheckOutRequest {

    @NotNull
    private Long attendanceRecordId;

    @NotNull
    private Double latitude;

    @NotNull
    private Double longitude;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime capturedAt;

    private String checkOutReason;
}
