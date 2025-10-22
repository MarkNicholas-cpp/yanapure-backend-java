package com.yanapure.app.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yanapure.app.users.Role;
import com.yanapure.app.users.User;
import java.time.Instant;
import java.util.UUID;

/** Response DTO for authentication operations */
public class AuthResponse {

  @JsonProperty("user")
  private UserDto user;

  @JsonProperty("access_token")
  private String accessToken;

  @JsonProperty("refresh_token")
  private String refreshToken;

  @JsonProperty("expires_in")
  private long expiresIn;

  @JsonProperty("token_type")
  private String tokenType = "Bearer";

  @JsonProperty("session_id")
  private UUID sessionId;

  // Constructors
  public AuthResponse() {}

  public AuthResponse(
      User user, String accessToken, String refreshToken, long expiresIn, UUID sessionId) {
    this.user = new UserDto(user);
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
    this.expiresIn = expiresIn;
    this.sessionId = sessionId;
  }

  // Getters and Setters
  public UserDto getUser() {
    return user;
  }

  public void setUser(UserDto user) {
    this.user = user;
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

  public long getExpiresIn() {
    return expiresIn;
  }

  public void setExpiresIn(long expiresIn) {
    this.expiresIn = expiresIn;
  }

  public String getTokenType() {
    return tokenType;
  }

  public void setTokenType(String tokenType) {
    this.tokenType = tokenType;
  }

  public UUID getSessionId() {
    return sessionId;
  }

  public void setSessionId(UUID sessionId) {
    this.sessionId = sessionId;
  }

  /** User DTO for response */
  public static class UserDto {
    @JsonProperty("id")
    private UUID id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("phone")
    private String phone;

    @JsonProperty("email")
    private String email;

    @JsonProperty("role")
    private Role role;

    @JsonProperty("last_login_at")
    private Instant lastLoginAt;

    @JsonProperty("created_at")
    private Instant createdAt;

    public UserDto() {}

    public UserDto(User user) {
      this.id = user.getId();
      this.name = user.getName();
      this.phone = user.getPhone();
      this.email = user.getEmail();
      this.role = user.getRole();
      this.lastLoginAt = user.getLastLoginAt();
      this.createdAt = user.getCreatedAt();
    }

    // Getters and Setters
    public UUID getId() {
      return id;
    }

    public void setId(UUID id) {
      this.id = id;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getPhone() {
      return phone;
    }

    public void setPhone(String phone) {
      this.phone = phone;
    }

    public String getEmail() {
      return email;
    }

    public void setEmail(String email) {
      this.email = email;
    }

    public Role getRole() {
      return role;
    }

    public void setRole(Role role) {
      this.role = role;
    }

    public Instant getLastLoginAt() {
      return lastLoginAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
      this.lastLoginAt = lastLoginAt;
    }

    public Instant getCreatedAt() {
      return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
      this.createdAt = createdAt;
    }
  }
}
