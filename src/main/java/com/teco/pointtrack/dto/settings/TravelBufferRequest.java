package com.teco.pointtrack.dto.settings;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TravelBufferRequest {

    /**
     * Thời gian đệm di chuyển tối thiểu giữa 2 ca liên tiếp (phút).
     * BR-09: mặc định 15 phút.
     */
    @NotNull
    @Min(value = 0, message = "Travel buffer phải >= 0 phút")
    Integer minutes;
}