package com.yanapure.app.auth.service;

import com.yanapure.app.users.Role;
import com.yanapure.app.users.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for JWT token generation and validation
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    @Value("${app.jwt.secret:yanapure-secret-key-change-in-production-very-long-and-secure}")
    private String jwtSecret;

    @Value("${app.jwt.access-token-expiry-hours:1}")
    private int accessTokenExpiryHours;

    @Value("${app.jwt.refresh-token-expiry-days:7}")
    private int refreshTokenExpiryDays;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    /**
     * Generate access token for user
     */
    public String generateAccessToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("phone", user.getPhone());
        claims.put("role", user.getRole().name());
        claims.put("tokenType", "access");

        Instant now = Instant.now();
        Instant expiry = now.plus(accessTokenExpiryHours, ChronoUnit.HOURS);

        return Jwts.builder()
                .claims(claims)
                .subject(user.getId().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Generate refresh token for user
     */
    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId().toString());
        claims.put("tokenType", "refresh");

        Instant now = Instant.now();
        Instant expiry = now.plus(refreshTokenExpiryDays, ChronoUnit.DAYS);

        return Jwts.builder()
                .claims(claims)
                .subject(user.getId().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Validate and parse JWT token
     */
    public Claims validateAndParseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
            throw new RuntimeException("Token expired");
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
            throw new RuntimeException("Unsupported token");
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
            throw new RuntimeException("Malformed token");
        } catch (SecurityException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
            throw new RuntimeException("Invalid token signature");
        } catch (IllegalArgumentException e) {
            log.warn("JWT token compact of handler are invalid: {}", e.getMessage());
            throw new RuntimeException("Invalid token");
        }
    }

    /**
     * Extract user ID from token
     */
    public String getUserIdFromToken(String token) {
        Claims claims = validateAndParseToken(token);
        return claims.getSubject();
    }

    /**
     * Extract phone number from token
     */
    public String getPhoneFromToken(String token) {
        Claims claims = validateAndParseToken(token);
        return claims.get("phone", String.class);
    }

    /**
     * Extract role from token
     */
    public Role getRoleFromToken(String token) {
        Claims claims = validateAndParseToken(token);
        String roleStr = claims.get("role", String.class);
        return Role.valueOf(roleStr);
    }

    /**
     * Check if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = validateAndParseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Get token expiration time
     */
    public Instant getTokenExpiration(String token) {
        Claims claims = validateAndParseToken(token);
        return claims.getExpiration().toInstant();
    }

    /**
     * Check if token is access token
     */
    public boolean isAccessToken(String token) {
        try {
            Claims claims = validateAndParseToken(token);
            return "access".equals(claims.get("tokenType", String.class));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if token is refresh token
     */
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = validateAndParseToken(token);
            return "refresh".equals(claims.get("tokenType", String.class));
        } catch (Exception e) {
            return false;
        }
    }
}
