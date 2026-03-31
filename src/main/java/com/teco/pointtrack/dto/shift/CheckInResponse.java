package com.teco.pointtrack.dto.shift;

import com.teco.pointtrack.entity.enums.ShiftStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CheckInResponse {

    /** true nếu nhân viên nằm trong vùng geofence cho phép */
    boolean withinGeofence;

    /** Khoảng cách thực tế tính theo Haversine (meters) */
    double distanceMeters;

    /** Bán kính geofence (meters). -1 nếu khách hàng chưa có toạ độ */
    double geofenceRadiusMeters;

    /** Thời điểm check-in hoặc check-out thực tế */
    LocalDateTime actionTime;

    ShiftStatus shiftStatus;
    String      message;
}
