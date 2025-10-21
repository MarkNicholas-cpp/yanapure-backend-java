package com.yanapure.app.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yanapure.app.auth.session.UserSession;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for user session information
 */
public class UserSessionDto {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("client_ip")
    private String clientIp;

    @JsonProperty("user_agent")
    private String userAgent;

    @JsonProperty("active")
    private boolean active;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("last_used_at")
    private Instant lastUsedAt;

    @JsonProperty("expires_at")
    private Instant expiresAt;

    // Constructors
    public UserSessionDto() {
    }

    public UserSessionDto(UserSession session) {
        this.id = session.getId();
        this.clientIp = session.getClientIp();
        this.userAgent = session.getUserAgent();
        this.active = session.getActive();
        this.createdAt = session.getCreatedAt();
        this.lastUsedAt = session.getLastUsedAt();
        this.expiresAt = session.getExpiresAt();
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public boolean isActive() {
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

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
