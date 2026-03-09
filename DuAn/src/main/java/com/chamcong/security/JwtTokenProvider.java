package com.chamcong.security;

import com.chamcong.config.JwtConfig;
import com.chamcong.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtConfig jwtConfig;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String BLACKLIST_PREFIX = "blacklist:";

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtConfig.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(UserDetails userDetails) {
        if (userDetails instanceof User user) {
            return generateAccessToken(user);
        }
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtConfig.getAccessExpiration());
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .id(jti)
                .subject(userDetails.getUsername())
                .claim("roles", userDetails.getAuthorities().stream()
                        .map(Object::toString).toList())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    public String generateAccessToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtConfig.getAccessExpiration());
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .id(jti)
                .subject(user.getEmail())
                .claim("userId", user.getId().toString())
                .claim("role", user.getRole().name())
                .claim("roles", user.getAuthorities().stream()
                        .map(Object::toString).toList())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
    }

    public String getEmailFromToken(String token) {
        return getClaims(token).getSubject();
    }

    public String getJtiFromToken(String token) {
        return getClaims(token).getId();
    }

    public Date getExpirationFromToken(String token) {
        return getClaims(token).getExpiration();
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = getClaims(token);
            String jti = claims.getId();
            if (jti != null && isTokenBlacklisted(jti)) {
                return false;
            }
            return !claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException | MalformedJwtException |
                 UnsupportedJwtException | SignatureException | IllegalArgumentException e) {
            return false;
        }
    }

    public void blacklistToken(String token) {
        try {
            Claims claims = getClaims(token);
            String jti = claims.getId();
            if (jti != null) {
                long ttl = claims.getExpiration().getTime() - System.currentTimeMillis();
                if (ttl > 0) {
                    redisTemplate.opsForValue().set(BLACKLIST_PREFIX + jti, "revoked", ttl, TimeUnit.MILLISECONDS);
                }
            }
        } catch (ExpiredJwtException e) {
            // token already expired
        }
    }

    public boolean isTokenBlacklisted(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + jti));
    }

    public long getAccessExpiration() {
        return jwtConfig.getAccessExpiration();
    }

    public long getRefreshExpiration() {
        return jwtConfig.getRefreshExpiration();
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

