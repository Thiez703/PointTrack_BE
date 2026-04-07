package com.teco.pointtrack.dto.shiftswap;

import jakarta.validation.constraints.NotBlank;

public record RejectSwapRequest(

        @NotBlank(message = "Lý do từ chối không được để trống")
        String rejectReason
) {}
