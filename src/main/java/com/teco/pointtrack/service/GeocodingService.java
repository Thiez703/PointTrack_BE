package com.teco.pointtrack.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class GeocodingService {

    private final String apiKey;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public GeocodingService(
            @Value("${app.google.maps-api-key:}") String apiKey,
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Geocode địa chỉ → [latitude, longitude]
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Gọi Google Maps Geocoding API để lấy toạ độ từ địa chỉ.
     *
     * @return double[]{latitude, longitude} hoặc null nếu thất bại
     */
    public double[] geocode(String street, String ward, String district, String city) {

        // Nếu apiKey blank → bỏ qua
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Google Maps API key is not configured, skipping geocode");
            return null;
        }

        // Nếu tất cả address fields đều null/blank → bỏ qua
        if (isAllBlank(street, ward, district, city)) {
            return null;
        }

        // Build địa chỉ đầy đủ: ghép các phần không null bằng ", "
        String address = Stream.of(street, ward, district, city)
                .filter(Objects::nonNull)
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining(", "));

        try {
            String response = webClient.get()
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

            JsonNode root = objectMapper.readTree(response);
            String status = root.path("status").asText();

            if (!"OK".equals(status)) {
                log.warn("Geocoding API returned status={} for address='{}'", status, address);
                return null;
            }

            JsonNode results = root.path("results");
            if (results.isEmpty()) {
                log.warn("Geocoding API returned empty results for address='{}'", address);
                return null;
            }

            JsonNode location = results.get(0)
                    .path("geometry").path("location");
            double lat = location.path("lat").asDouble();
            double lng = location.path("lng").asDouble();

            log.debug("Geocoded '{}' → lat={}, lng={}", address, lat, lng);
            return new double[]{lat, lng};

        } catch (Exception e) {
            log.error("Geocoding failed for address='{}': {}", address, e.getMessage());
            return null;
        }
    }

    private boolean isAllBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return false;
            }
        }
        return true;
    }
}

