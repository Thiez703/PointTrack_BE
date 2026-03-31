package com.teco.pointtrack.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teco.pointtrack.dto.customer.GeoPoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Dịch vụ geocoding địa chỉ → tọa độ GPS thông qua Google Maps Geocoding API.
 *
 * <h3>Tính năng</h3>
 * <ul>
 *   <li>Cache kết quả trong Redis (TTL 7 ngày) để tránh gọi API trùng lặp</li>
 *   <li>Tự động retry tối đa 3 lần khi gặp OVER_QUERY_LIMIT</li>
 *   <li>Thêm ", Việt Nam" vào cuối địa chỉ để tăng độ chính xác</li>
 *   <li>Hỗ trợ {@code @Async} cho import hàng loạt</li>
 * </ul>
 */
@Service
@Slf4j
public class GeocodingService {

    private static final String CACHE_PREFIX     = "geocode:";
    private static final Duration CACHE_TTL      = Duration.ofDays(7);
    private static final int MAX_RETRY            = 3;
    private static final long RETRY_DELAY_MS      = 1_000;

    private static final String STATUS_OK                = "OK";
    private static final String STATUS_ZERO_RESULTS      = "ZERO_RESULTS";
    private static final String STATUS_OVER_QUERY_LIMIT  = "OVER_QUERY_LIMIT";
    private static final String STATUS_REQUEST_DENIED    = "REQUEST_DENIED";

    private final String apiKey;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    public GeocodingService(
            @Value("${app.google.maps-api-key:}") String apiKey,
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            StringRedisTemplate redisTemplate) {

        this.apiKey        = apiKey;
        this.webClient     = webClientBuilder.build();
        this.objectMapper  = objectMapper;
        this.redisTemplate = redisTemplate;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Geocode địa chỉ dạng chuỗi đầy đủ → {@link GeoPoint}.
     * Kết quả được cache trong Redis (TTL 7 ngày).
     *
     * @param address Địa chỉ đầy đủ (VD: "123 Nguyễn Văn A, Phường 1, Quận 1, TP.HCM")
     * @return {@link GeoPoint} hoặc {@code null} nếu geocoding thất bại
     */
    public GeoPoint geocode(String address) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Google Maps API key chưa được cấu hình, bỏ qua geocoding");
            return null;
        }
        if (address == null || address.isBlank()) {
            return null;
        }

        String normalizedAddress = address.trim();

        // Kiểm tra cache trước
        String cacheKey = buildCacheKey(normalizedAddress);
        String cached   = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return parseGeoPoint(cached);
        }

        // Thêm Việt Nam vào địa chỉ để cải thiện kết quả
        String queryAddress = normalizedAddress.contains("Việt Nam")
                ? normalizedAddress
                : normalizedAddress + ", Việt Nam";

        GeoPoint result = callGoogleMapsWithRetry(queryAddress);

        if (result != null) {
            // Lưu cache
            redisTemplate.opsForValue().set(cacheKey, result.latitude() + "," + result.longitude(), CACHE_TTL);
            log.debug("Geocoded '{}' → lat={}, lng={}", normalizedAddress, result.latitude(), result.longitude());
        }

        return result;
    }

    /**
     * Geocode địa chỉ từ 4 thành phần (backward-compatible với code cũ).
     *
     * @return {@link GeoPoint} hoặc {@code null} nếu thất bại
     */
    public GeoPoint geocode(String street, String ward, String district, String city) {
        String address = buildAddress(street, ward, district, city);
        if (address == null || address.isBlank()) return null;
        return geocode(address);
    }

    /**
     * Phiên bản bất đồng bộ — dùng khi import hàng loạt (batch geocoding).
     *
     * @param address Địa chỉ đầy đủ
     * @return {@link CompletableFuture} chứa {@link GeoPoint} hoặc {@code null}
     */
    @Async
    public CompletableFuture<GeoPoint> geocodeAsync(String address) {
        return CompletableFuture.completedFuture(geocode(address));
    }

    /**
     * Geocode và trả về formatted address từ Google Maps.
     * Dùng cho endpoint re-geocode.
     *
     * @return mảng: [latitude, longitude, formattedAddress] hoặc null nếu thất bại
     */
    public GeocodeFullResult geocodeFull(String address) {
        if (apiKey == null || apiKey.isBlank()) return null;
        if (address == null || address.isBlank()) return null;

        String queryAddress = address.trim().contains("Việt Nam")
                ? address.trim()
                : address.trim() + ", Việt Nam";

        try {
            String response = callGoogleMapsApi(queryAddress);
            if (response == null) return null;

            JsonNode root   = objectMapper.readTree(response);
            String   status = root.path("status").asText();

            if (!STATUS_OK.equals(status)) return null;

            JsonNode results = root.path("results");
            if (results.isEmpty()) return null;

            JsonNode result   = results.get(0);
            JsonNode location = result.path("geometry").path("location");
            String   formatted = result.path("formatted_address").asText(null);

            return new GeocodeFullResult(
                    location.path("lat").asDouble(),
                    location.path("lng").asDouble(),
                    formatted);

        } catch (Exception e) {
            log.error("geocodeFull failed for '{}': {}", address, e.getMessage());
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal
    // ─────────────────────────────────────────────────────────────────────────

    private GeoPoint callGoogleMapsWithRetry(String address) {
        int attempt = 0;
        while (attempt < MAX_RETRY) {
            try {
                String response = callGoogleMapsApi(address);
                if (response == null) return null;

                JsonNode root   = objectMapper.readTree(response);
                String   status = root.path("status").asText();

                switch (status) {
                    case STATUS_OK -> {
                        JsonNode results = root.path("results");
                        if (results.isEmpty()) return null;
                        JsonNode loc = results.get(0).path("geometry").path("location");
                        return new GeoPoint(loc.path("lat").asDouble(), loc.path("lng").asDouble());
                    }
                    case STATUS_ZERO_RESULTS -> {
                        log.warn("Geocoding: ZERO_RESULTS cho địa chỉ='{}'", address);
                        return null;
                    }
                    case STATUS_REQUEST_DENIED -> {
                        log.error("Geocoding: REQUEST_DENIED — kiểm tra API key và billing");
                        return null;
                    }
                    case STATUS_OVER_QUERY_LIMIT -> {
                        attempt++;
                        if (attempt < MAX_RETRY) {
                            log.warn("Geocoding: OVER_QUERY_LIMIT, thử lại lần {} sau {}ms", attempt, RETRY_DELAY_MS);
                            sleep(RETRY_DELAY_MS);
                        } else {
                            log.error("Geocoding: OVER_QUERY_LIMIT sau {} lần thử", MAX_RETRY);
                            return null;
                        }
                    }
                    default -> {
                        log.warn("Geocoding: status không xác định='{}' cho địa chỉ='{}'", status, address);
                        return null;
                    }
                }

            } catch (WebClientResponseException e) {
                log.error("Geocoding HTTP error {}: {}", e.getStatusCode(), e.getMessage());
                return null;
            } catch (Exception e) {
                log.error("Geocoding exception cho '{}': {}", address, e.getMessage());
                return null;
            }
        }
        return null;
    }

    private String callGoogleMapsApi(String address) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("maps.googleapis.com")
                        .path("/maps/api/geocode/json")
                        .queryParam("address", address)
                        .queryParam("key", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    private String buildCacheKey(String address) {
        String hash = DigestUtils.md5DigestAsHex(
                address.toLowerCase().trim().getBytes(StandardCharsets.UTF_8));
        return CACHE_PREFIX + hash;
    }

    private GeoPoint parseGeoPoint(String cached) {
        try {
            String[] parts = cached.split(",");
            return new GeoPoint(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]));
        } catch (Exception e) {
            log.warn("Lỗi parse GeoPoint từ cache: '{}'", cached);
            return null;
        }
    }

    private String buildAddress(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part != null && !part.isBlank()) {
                if (!sb.isEmpty()) sb.append(", ");
                sb.append(part.trim());
            }
        }
        return sb.toString();
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    // ── Inner result type ─────────────────────────────────────────────────────

    public record GeocodeFullResult(double latitude, double longitude, String formattedAddress) {}
}
