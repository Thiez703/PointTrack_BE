package com.teco.pointtrack.dto.shift;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CopyWeekRequest {

    @NotBlank
    @Pattern(regexp = "^\\d{4}-W(0[1-9]|[1-4]\\d|5[0-3])$",
             message = "Định dạng tuần phải là 'yyyy-Www', VD: 2026-W12")
    String sourceWeek;

    @NotBlank
    @Pattern(regexp = "^\\d{4}-W(0[1-9]|[1-4]\\d|5[0-3])$",
             message = "Định dạng tuần phải là 'yyyy-Www', VD: 2026-W13")
    String targetWeek;
}
