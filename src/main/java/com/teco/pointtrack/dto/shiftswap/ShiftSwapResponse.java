package com.teco.pointtrack.dto.shiftswap;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.teco.pointtrack.entity.enums.SwapStatus;
import com.teco.pointtrack.entity.enums.SwapType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShiftSwapResponse {

    Long id;
    SwapType type;
    SwapStatus status;

    // ── NV yêu cầu ──────────────────────────────────────────────────────────
    UserInfo requester;

    // ── Ca của NV_A ──────────────────────────────────────────────────────────
    ShiftInfo requesterShift;

    // ── NV nhận (null nếu gửi Admin) ─────────────────────────────────────────
    UserInfo receiver;

    // ── Ca của NV_B (SWAP) ───────────────────────────────────────────────────
    ShiftInfo receiverShift;

    // ── Ca mục tiêu (SAME_DAY / OTHER_DAY) ──────────────────────────────────
    ShiftInfo targetShift;

    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate targetDate;

    String reason;
    String rejectReason;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime expiredAt;

    UserInfo reviewedBy;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime reviewedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime updatedAt;

    // ── Nested types ──────────────────────────────────────────────────────────

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class UserInfo {
        Long id;
        String fullName;
        String avatarUrl;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ShiftInfo {
        Long id;

        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate shiftDate;

        @JsonFormat(pattern = "HH:mm")
        LocalTime startTime;

        @JsonFormat(pattern = "HH:mm")
        LocalTime endTime;

        String customerName;
        String customerAddress;
    }
}
