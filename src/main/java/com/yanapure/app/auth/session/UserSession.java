package com.yanapure.app.auth.session;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * User session entity to track authentication state
 * Stores JWT tokens, refresh tokens, and session metadata
 */
@Entity
@Table(name = "user_sessions", indexes = {
        @Index(name = "idx_sessions_user", columnList = "userId"),
        @Index(name = "idx_sessions_token", columnList = "accessToken"),
        @Index(name = "idx_sessions_refresh", columnList = "refreshToken"),
        @Index(name = "idx_sessions_expires", columnList = "expiresAt")
})
public class UserSession {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 500)
    private String accessToken;

    @Column(nullable = false, length = 500)
    private String refreshToken;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Instant refreshExpiresAt;

    @Column(length = 45)
    private String clientIp;

    @Column(length = 500)
    private String userAgent;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant lastUsedAt;

    // Constructors
    public UserSession() {
    }

    public UserSession(UUID userId, String accessToken, String refreshToken,
            Instant expiresAt, Instant refreshExpiresAt,
            String clientIp, String userAgent) {
        this.userId = userId;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresAt = expiresAt;
        this.refreshExpiresAt = refreshExpiresAt;
        this.clientIp = clientIp;
        this.userAgent = userAgent;
    }

    // Business methods
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isRefreshExpired() {
        return Instant.now().isAfter(refreshExpiresAt);
    }

    public boolean isActive() {
        return active && !isExpired();
    }

    public void deactivate() {
        this.active = false;
    }

    public void updateLastUsed() {
        this.lastUsedAt = Instant.now();
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getRefreshExpiresAt() {
        return refreshExpiresAt;
    }

    public void setRefreshExpiresAt(Instant refreshExpiresAt) {
        this.refreshExpiresAt = refreshExpiresAt;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public boolean getActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(Instant lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }
}
