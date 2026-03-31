package com.teco.pointtrack.dto.customer;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Response cho POST /api/v1/customers/{id}/geocode — Re-geocode địa chỉ.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GeocodeResponse {

    boolean success;
    Double latitude;
    Double longitude;

    /** Địa chỉ chuẩn hoá từ Google Maps */
    String formattedAddress;

    String message;
}
