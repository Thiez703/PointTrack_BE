package com.teco.pointtrack.dto.attendance;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Response cho GET /attendance/locations — dropdown chọn địa điểm.
 */
@Getter
@AllArgsConstructor
public class LocationDropdownResponse {
    Long id;
    String name;
}
