package com.teco.pointtrack.utils;

/**
 * Haversine formula — tính khoảng cách (meters) giữa 2 tọa độ GPS trên mặt cầu.
 *
 * Độ chính xác: ~0.3% (đủ dùng cho GPS fencing bán kính 50m trở lên).
 * Không dùng cho: tính đường bay dài hơn 200km (cần Vincenty formula).
 */
public final class GpsUtils {

    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    private GpsUtils() {}

    /**
     * @param lat1 Vĩ độ điểm 1 (degrees)
     * @param lng1 Kinh độ điểm 1 (degrees)
     * @param lat2 Vĩ độ điểm 2 (degrees)
     * @param lng2 Kinh độ điểm 2 (degrees)
     * @return Khoảng cách tính bằng meters
     */
    public static double distanceMeters(double lat1, double lng1,
                                        double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }

    /** Alias cho {@link #distanceMeters} — tên phù hợp với spec BR-14. */
    public static double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        return distanceMeters(lat1, lng1, lat2, lng2);
    }

    /**
     * Kiểm tra điểm (lat, lng) có nằm trong bán kính (meters) của tâm không.
     */
    public static boolean isWithinRadius(double lat, double lng,
                                         double centerLat, double centerLng,
                                         double radiusMeters) {
        return distanceMeters(lat, lng, centerLat, centerLng) <= radiusMeters;
    }
}
