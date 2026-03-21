package com.teco.pointtrack.dto.attendance;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ApprovalRequest {

    /** Ghi chú của Admin khi duyệt/từ chối (optional) */
    String reviewNote;
}
