package com.teco.pointtrack.dto.customer;

import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

/**
 * Body cho PUT /api/v1/customers/{id}/gps — Cập nhật GPS thủ công.
 */
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GpsUpdateRequest {

    @NotNull(message = "Latitude không được để trống")
    @DecimalMin(value = "-90.0",  message = "Latitude phải từ -90 đến 90")
    @DecimalMax(value = "90.0",   message = "Latitude phải từ -90 đến 90")
    Double latitude;

    @NotNull(message = "Longitude không được để trống")
    @DecimalMin(value = "-180.0", message = "Longitude phải từ -180 đến 180")
    @DecimalMax(value = "180.0",  message = "Longitude phải từ -180 đến 180")
    Double longitude;
}
