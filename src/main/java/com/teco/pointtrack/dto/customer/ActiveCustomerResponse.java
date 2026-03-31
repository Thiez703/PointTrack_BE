package com.teco.pointtrack.dto.customer;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Response cho GET /api/v1/customers/active-with-gps — Dùng cho dropdown tạo ca.
 * Chỉ trả về KH ACTIVE có tọa độ GPS.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ActiveCustomerResponse {

    Long id;
    String name;
    String phone;
    String address;
    Double latitude;
    Double longitude;
}
