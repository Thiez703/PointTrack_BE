package com.teco.pointtrack.dto.shiftswap;

public record RespondSwapRequest(

        boolean accept,

        /** Bắt buộc khi accept = false */
        String rejectReason
) {}
