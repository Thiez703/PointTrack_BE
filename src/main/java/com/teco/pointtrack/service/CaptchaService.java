package com.teco.pointtrack.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CaptchaService {

    private static final Logger log = LoggerFactory.getLogger(CaptchaService.class);

    @Value("${app.turnstile.secret-key}")
    private String secretKey;

    @Value("${app.turnstile.url}")
    private String verifyUrl;

    private final RestTemplate restTemplate;

    public boolean verifyCaptcha(String token, HttpServletRequest servletRequest) {

        // ─────────────────────────────────────────────────────────────────────────────
        // BYPASS TRÊN LOCAL / TEST (DC-15)
        // Nếu dùng key test của Cloudflare hoặc token đặc biệt thì cho qua
        // ─────────────────────────────────────────────────────────────────────────────
        if (secretKey != null && secretKey.trim().startsWith("1x000") || "bypass-local".equals(token)) {
            return true;
        }

        if (token == null || token.isBlank()) {
            return false;
        }

        String clientIp = getClientIp(servletRequest);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("secret", secretKey != null ? secretKey.trim() : "");
        body.add("response", token);
        body.add("remoteip", clientIp);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        TurnstileResponse response;
        try {
            response = restTemplate.postForObject(verifyUrl, request, TurnstileResponse.class);
        } catch (Exception e) {
            log.error("Captcha verification failed: {}", e.getMessage(), e);
            return false;
        }

        return response != null && response.success;
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null && !xfHeader.isBlank()) {
            return xfHeader.split(",")[0];
        }
        return request.getRemoteAddr();
    }

    @Data
    private static class TurnstileResponse {
        private boolean success;

        @JsonProperty("error-codes")
        private List<String> errorCodes;
    }
}
