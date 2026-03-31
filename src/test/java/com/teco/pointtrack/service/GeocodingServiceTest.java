package com.teco.pointtrack.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teco.pointtrack.dto.customer.GeoPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GeocodingServiceTest {

    @Mock WebClient.Builder    webClientBuilder;
    @Mock WebClient            webClient;
    @Mock WebClient.RequestHeadersUriSpec<?> uriSpec;
    @Mock WebClient.RequestHeadersSpec<?>    headersSpec;
    @Mock WebClient.ResponseSpec             responseSpec;
    @Mock StringRedisTemplate  redisTemplate;
    @Mock ValueOperations<String, String>    valueOps;

    ObjectMapper       objectMapper = new ObjectMapper();
    GeocodingService   service;

    private static final String API_KEY   = "test-api-key";
    private static final String TEST_ADDR = "123 Nguyễn Văn A, Quận 1, TP.HCM";

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        when(webClientBuilder.build()).thenReturn(webClient);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        service = new GeocodingService(API_KEY, webClientBuilder, objectMapper, redisTemplate);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void mockApiResponse(String jsonResponse) {
        when(webClient.get()).thenReturn((WebClient.RequestHeadersUriSpec) uriSpec);
        when(uriSpec.uri(any(java.util.function.Function.class))).thenReturn((WebClient.RequestHeadersSpec) headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(jsonResponse));
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("geocode: Cache hit → trả về GeoPoint từ Redis, không gọi API")
    void geocode_cacheHit_returnsCachedGeoPoint() {
        when(valueOps.get(anyString())).thenReturn("10.7769," + "106.7009");

        GeoPoint result = service.geocode(TEST_ADDR);

        assertThat(result).isNotNull();
        assertThat(result.latitude()).isEqualTo(10.7769);
        assertThat(result.longitude()).isEqualTo(106.7009);
        verify(webClient, never()).get(); // API không được gọi
    }

    @Test
    @DisplayName("geocode: API trả về OK → trả về GeoPoint và lưu cache")
    void geocode_apiOk_returnsGeoPointAndCaches() {
        when(valueOps.get(anyString())).thenReturn(null);
        mockApiResponse(okResponse("10.7769", "106.7009"));

        GeoPoint result = service.geocode(TEST_ADDR);

        assertThat(result).isNotNull();
        assertThat(result.latitude()).isEqualTo(10.7769);
        assertThat(result.longitude()).isEqualTo(106.7009);
        verify(valueOps).set(anyString(), eq("10.7769,106.7009"), any(Duration.class));
    }

    @Test
    @DisplayName("geocode: ZERO_RESULTS → trả về null, không lưu cache")
    void geocode_zeroResults_returnsNull() {
        when(valueOps.get(anyString())).thenReturn(null);
        mockApiResponse("{\"status\":\"ZERO_RESULTS\",\"results\":[]}");

        GeoPoint result = service.geocode(TEST_ADDR);

        assertThat(result).isNull();
        verify(valueOps, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("geocode: REQUEST_DENIED → trả về null")
    void geocode_requestDenied_returnsNull() {
        when(valueOps.get(anyString())).thenReturn(null);
        mockApiResponse("{\"status\":\"REQUEST_DENIED\",\"results\":[]}");

        GeoPoint result = service.geocode(TEST_ADDR);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("geocode: API key trống → trả về null ngay lập tức, không gọi API")
    void geocode_noApiKey_returnsNull() {
        GeocodingService noKeyService =
                new GeocodingService("", webClientBuilder, objectMapper, redisTemplate);

        GeoPoint result = noKeyService.geocode(TEST_ADDR);

        assertThat(result).isNull();
        verify(webClient, never()).get();
    }

    @Test
    @DisplayName("geocode: địa chỉ null → trả về null ngay lập tức")
    void geocode_nullAddress_returnsNull() {
        GeoPoint result = service.geocode((String) null);
        assertThat(result).isNull();
        verify(webClient, never()).get();
    }

    @Test
    @DisplayName("geocode: địa chỉ rỗng → trả về null ngay lập tức")
    void geocode_blankAddress_returnsNull() {
        GeoPoint result = service.geocode("   ");
        assertThat(result).isNull();
        verify(webClient, never()).get();
    }

    @Test
    @DisplayName("geocode: exception từ WebClient → trả về null (graceful)")
    void geocode_webClientException_returnsNull() {
        when(valueOps.get(anyString())).thenReturn(null);
        when(webClient.get()).thenThrow(new RuntimeException("Network error"));

        GeoPoint result = service.geocode(TEST_ADDR);

        assertThat(result).isNull();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String okResponse(String lat, String lng) {
        return """
                {
                  "status": "OK",
                  "results": [{
                    "geometry": {
                      "location": { "lat": %s, "lng": %s }
                    },
                    "formatted_address": "123 Nguyễn Văn A, Phường 1, Quận 1, TP.HCM, Việt Nam"
                  }]
                }
                """.formatted(lat, lng);
    }
}
