package com.teco.pointtrack.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Gọi Google Maps Distance Matrix API để lấy thời gian di chuyển thực tế giữa 2 toạ độ.
 * Dùng cho BR-09: thay buffer cố định 15 phút bằng thời gian thực tế.
 */
@Service
@Slf4j
public class TravelTimeService {

    private final String       apiKey;
    private final WebClient    webClient;
    private final ObjectMapper objectMapper;

    public TravelTimeService(
            @Value("${app.google.maps-api-key:}") String apiKey,
            WebClient.Builder builder,
            ObjectMapper objectMapper) {
        this.apiKey       = apiKey;
        this.webClient    = builder.build();
        this.objectMapper = objectMapper;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Lấy thời gian lái xe từ origin → destination (phút, làm tròn lên).
     * <p>
     * Nếu API key chưa cấu hình, toạ độ null, hoặc gọi thất bại →
     * trả về {@code fallbackMinutes} để không làm crash luồng kiểm tra conflict.
     *
     * @param originLat       vĩ độ điểm xuất phát
     * @param originLng       kinh độ điểm xuất phát
     * @param destLat         vĩ độ điểm đến
     * @param destLng         kinh độ điểm đến
     * @param fallbackMinutes giá trị fallback (thường là BUFFER_MINUTES = 15)
     */
    public int getTravelMinutes(double originLat, double originLng,
                                double destLat,   double destLng,
                                int fallbackMinutes) {

        if (apiKey == null || apiKey.isBlank()) {
            log.debug("Google Maps API key chưa cấu hình, dùng buffer cố định {}min", fallbackMinutes);
            return fallbackMinutes;
        }

        try {
            String origins      = originLat + "," + originLng;
            String destinations = destLat   + "," + destLng;

            String raw = webClient.get()
                    .uri(uri -> uri
                            .scheme("https")
                            .host("maps.googleapis.com")
                            .path("/maps/api/distancematrix/json")
                            .queryParam("origins",      origins)
                            .queryParam("destinations", destinations)
                            .queryParam("mode",         "driving")
                            .queryParam("key",          apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root   = objectMapper.readTree(raw);
            String   status = root.path("status").asText();

            if (!"OK".equals(status)) {
                log.warn("Distance Matrix API status={} ({} → {})", status, origins, destinations);
                return fallbackMinutes;
            }

            JsonNode element    = root.path("rows").get(0).path("elements").get(0);
            String   elemStatus = element.path("status").asText();

            if (!"OK".equals(elemStatus)) {
                log.warn("Distance Matrix element status={}", elemStatus);
                return fallbackMinutes;
            }

            int durationSec = element.path("duration").path("value").asInt();
            int minutes     = (int) Math.ceil(durationSec / 60.0);

            log.debug("Thời gian di chuyển {} → {}: {}min", origins, destinations, minutes);
            return minutes;

        } catch (Exception e) {
            log.error("Distance Matrix API lỗi: {}", e.getMessage());
            return fallbackMinutes;
        }
    }
}
