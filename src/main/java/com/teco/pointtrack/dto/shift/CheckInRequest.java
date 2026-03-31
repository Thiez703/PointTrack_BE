package com.teco.pointtrack.dto.shift;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CheckInRequest {

    @NotNull(message = "latitude không được để trống")
    @Schema(example = "10.7769")
    Double latitude;

    @NotNull(message = "longitude không được để trống")
    @Schema(example = "106.7009")
    Double longitude;

    /** URL ảnh hiện trường (bắt buộc nếu ngoài geofence hoặc đi muộn, upload trước qua /api/v1/files) */
    String photoUrl;
}
