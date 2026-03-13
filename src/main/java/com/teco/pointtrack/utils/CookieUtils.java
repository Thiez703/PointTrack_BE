package com.teco.pointtrack.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletResponse;

@Component
public class CookieUtils {

    public static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    @Value("${security.jwt.expiry-time-in-seconds}")
    private long accessExpirationSeconds;

    @Value("${security.jwt.refreshable-duration}")
    private long refreshExpirationSeconds;

    @Value("${app.cookie.secure:false}")
    private boolean isSecure;

    @Value("${app.cookie.same-site:Lax}")
    private String sameSite;

    public void setAuthCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        ResponseCookie accessCookie = buildCookie(ACCESS_TOKEN_COOKIE_NAME, accessToken, accessExpirationSeconds);
        ResponseCookie refreshCookie = buildCookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken, refreshExpirationSeconds);
        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

    public void deleteAuthCookies(HttpServletResponse response) {
        ResponseCookie deleteAccess = buildCookie(ACCESS_TOKEN_COOKIE_NAME, "", 0);
        ResponseCookie deleteRefresh = buildCookie(REFRESH_TOKEN_COOKIE_NAME, "", 0);
        response.addHeader(HttpHeaders.SET_COOKIE, deleteAccess.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, deleteRefresh.toString());
    }

    private ResponseCookie buildCookie(String name, String value, long maxAgeSeconds) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(isSecure)
                .path("/")
                .maxAge(maxAgeSeconds)
                .sameSite(sameSite)
                .build();
    }
}
