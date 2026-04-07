package com.teco.pointtrack.utils;

import com.teco.pointtrack.exception.CustomAuthenticationException;
import com.teco.pointtrack.security.CustomUserDetail;
import com.teco.pointtrack.security.CustomUserDetailsService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class JwtUtils {

    @Value("${security.jwt.secret-key}")
    private String secret;

    @Value("${security.jwt.issuer}")
    private String issuer;

    @Value("${security.jwt.expiry-time-in-seconds}")
    private Long accessExpiration;

    @Value("${security.jwt.refreshable-duration}")
    private Long refreshExpiration;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    private static final String TOKEN_PREFIX = "revoked_token:";
    private static final String BEARER_PREFIX = "Bearer ";

    public String generateAccessToken(UserDetails userDetails) {
        CustomUserDetail customUserDetail = (CustomUserDetail) userDetails;

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", customUserDetail.getUserDetail().getId());
        claims.put("fullName", customUserDetail.getUserDetail().getFullName());
        claims.put("roleSlug", customUserDetail.getUserDetail().getRole() != null
                ? customUserDetail.getUserDetail().getRole().getSlug() : null);

        return buildToken(customUserDetail.getUsername(), claims, accessExpiration);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(userDetails.getUsername(), new HashMap<>(), refreshExpiration);
    }

    public Authentication setAuthentication(String token) {
        Claims payload = parseClaimsFromToken(token);
        String username = payload.getSubject();
        CustomUserDetail customUserDetail = (CustomUserDetail) userDetailsService.loadUserByUsername(username);

        // Invalidate token nếu mật khẩu đã đổi sau khi token được cấp
        if (customUserDetail.getUserDetail().getPasswordChangedAt() != null) {
            long changedAt = java.sql.Timestamp.valueOf(customUserDetail.getUserDetail().getPasswordChangedAt()).getTime();
            long issuedAt = payload.getIssuedAt().getTime();

            // Cho phép sai số 1 giây để tránh lỗi làm tròn thời gian
            if (issuedAt < (changedAt - 1000)) {
                throw new CustomAuthenticationException("Mật khẩu đã được thay đổi. Vui lòng đăng nhập lại.");
            }
        }

        return new UsernamePasswordAuthenticationToken(customUserDetail, "", customUserDetail.getAuthorities());
    }

    public boolean validateToken(String token) {
        try {
            parseClaimsFromToken(token);
            return !isTokenExpired(token) && !isTokenRevoked(token);
        } catch (Exception e) {
            return false;
        }
    }

    public String extractUsername(String token) {
        Claims payload = parseClaimsFromToken(token);
        return payload.getSubject();
    }

    public boolean isTokenRevoked(String token) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(TOKEN_PREFIX + token));
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(JwtUtils.class)
                    .warn("Redis connection failed during token revocation check: {}", e.getMessage());
            // Fail gracefully - token not revoked if Redis is down
            return false;
        }
    }

    public void revokeToken(String token) {
        if (token == null || !validateToken(token)) {
            // Token null, hết hạn, hoặc không hợp lệ → không cần revoke, bỏ qua
            return;
        }
        try {
            Claims claims = parseClaimsFromToken(token);
            long remainingTime = claims.getExpiration().getTime() - System.currentTimeMillis();
            if (remainingTime > 0) {
                redisTemplate.opsForValue().set(TOKEN_PREFIX + token, "revoked", remainingTime, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            // Redis down hoặc lỗi khác → log nhưng không block logout
            org.slf4j.LoggerFactory.getLogger(JwtUtils.class)
                    .warn("Không thể revoke token trên Redis: {}", e.getMessage());
        }
    }

    private String buildToken(String subject, Map<String, Object> claims, long expiration) {
        return Jwts.builder()
                .issuer(issuer)
                .subject(subject)
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + (expiration * 1000)))
                .signWith(getSecretKey())
                .compact();
    }

    private SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    private Claims parseClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build().parseSignedClaims(token).getPayload();
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseClaimsFromToken(token);
            return claims.getExpiration().before(new Date());
        } catch (JwtException e) {
            return true;
        }
    }

    public String extractBearerToken(String bearerToken) {
        if (StringUtils.hasText(bearerToken)
                && bearerToken.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
