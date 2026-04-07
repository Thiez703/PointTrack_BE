package com.teco.pointtrack.dto.shiftswap;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.teco.pointtrack.entity.enums.SwapType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateShiftSwapRequest(

        @NotNull(message = "Loại đổi ca không được để trống")
        SwapType type,

        @NotNull(message = "Ca muốn đổi không được để trống")
        Long myShiftId,

        /** SWAP: bắt buộc; SAME_DAY / OTHER_DAY: optional (nếu có ca cụ thể) */
        Long targetShiftId,

        /** SWAP hoặc TRANSFER: bắt buộc */
        Long targetEmployeeId,

        /** OTHER_DAY: bắt buộc */
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate targetDate,

        @NotBlank(message = "Lý do đổi ca không được để trống")
        @Size(min = 10, max = 500, message = "Lý do phải từ 10 đến 500 ký tự")
        String reason
) {}
