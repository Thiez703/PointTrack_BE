package com.teco.pointtrack.dto.settings;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GracePeriodRequest {

    /**
     * Số phút cho phép check-in muộn vẫn tính đúng giờ.
     * Chỉ áp dụng cho check-in (BR-11). Mặc định 5 phút.
     */
    @NotNull
    @Min(value = 0, message = "Grace period phải >= 0 phút")
    Integer minutes;
}