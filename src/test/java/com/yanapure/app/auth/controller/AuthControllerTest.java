package com.yanapure.app.auth.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanapure.app.auth.dto.AuthRequest;
import com.yanapure.app.auth.service.AuthenticationService;
import com.yanapure.app.common.ApiException;
import com.yanapure.app.common.GlobalExceptionHandler;
import com.yanapure.app.users.Role;
import com.yanapure.app.users.User;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@TestPropertySource(
    properties = {
      "app.jwt.secret=test-secret-key-for-testing-only",
      "app.jwt.access-token-expiry-hours=1",
      "app.jwt.refresh-token-expiry-days=7",
      "app.auth.access-token-expiry-hours=1"
    })
public class AuthControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private AuthenticationService authenticationService;

  @Autowired private ObjectMapper objectMapper;

  @Test
  void testSendOtpSuccess() throws Exception {
    // Given
    AuthRequest request = new AuthRequest("+14155552671");

    // When & Then
    mockMvc
        .perform(
            post("/api/auth/send-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("Verification code sent successfully"));
  }

  @Test
  void testSendOtpWithInvalidPhone() throws Exception {
    // Given
    AuthRequest request = new AuthRequest("invalid-phone");
    doThrow(new ApiException("PHONE_INVALID", "Phone must be E.164 (e.g., +14155552671)"))
        .when(authenticationService)
        .initiatePhoneAuth(anyString(), anyString());

    // When & Then
    mockMvc
        .perform(
            post("/api/auth/send-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error").value("PHONE_INVALID"));
  }

  @Test
  void testSendOtpWithApiException() throws Exception {
    // Given
    AuthRequest request = new AuthRequest("+14155552671");
    doThrow(new ApiException("RATE_LIMIT_EXCEEDED", "Too many requests"))
        .when(authenticationService)
        .initiatePhoneAuth(anyString(), anyString());

    // When & Then
    mockMvc
        .perform(
            post("/api/auth/send-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error").value("RATE_LIMIT_EXCEEDED"));
  }

  @Test
  void testVerifyOtpSuccess() throws Exception {
    // Given
    User user = createTestUser();
    UUID sessionId = UUID.randomUUID();
    AuthenticationService.AuthResult result =
        new AuthenticationService.AuthResult(user, "access-token", "refresh-token", sessionId);

    when(authenticationService.verifyPhoneAndLogin(
            anyString(), anyString(), anyString(), anyString()))
        .thenReturn(result);

    // When & Then
    mockMvc
        .perform(
            post("/api/auth/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                            {"phone": "+14155552671", "otp": "123456"}
                        """)
                .header("X-Forwarded-For", "127.0.0.1")
                .header("User-Agent", "test-agent"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.id").value(user.getId().toString()))
        .andExpect(jsonPath("$.user.name").value(user.getName()))
        .andExpect(jsonPath("$.user.phone").value(user.getPhone()))
        .andExpect(jsonPath("$.user.role").value(user.getRole().toString()))
        .andExpect(jsonPath("$.access_token").value("access-token"))
        .andExpect(jsonPath("$.refresh_token").value("refresh-token"))
        .andExpect(jsonPath("$.expires_in").value(3600))
        .andExpect(jsonPath("$.token_type").value("Bearer"))
        .andExpect(jsonPath("$.session_id").value(sessionId.toString()));
  }

  @Test
  void testVerifyOtpWithInvalidOtp() throws Exception {
    // Given
    when(authenticationService.verifyPhoneAndLogin(
            anyString(), anyString(), anyString(), anyString()))
        .thenThrow(new ApiException("INVALID_OTP", "Invalid verification code"));

    // When & Then
    mockMvc
        .perform(
            post("/api/auth/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                            {"phone": "+14155552671", "otp": "000000"}
                        """)
                .header("X-Forwarded-For", "127.0.0.1")
                .header("User-Agent", "test-agent"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error").value("INVALID_OTP"))
        .andExpect(jsonPath("$.message").value("Invalid verification code"));
  }

  @Test
  void testRefreshTokenSuccess() throws Exception {
    // Given
    AuthRequest request = new AuthRequest();
    request.setRefreshToken("valid-refresh-token");
    User user = createTestUser();
    AuthenticationService.AuthResult result =
        new AuthenticationService.AuthResult(
            user, "new-access-token", "new-refresh-token", UUID.randomUUID());

    when(authenticationService.refreshToken(anyString(), anyString())).thenReturn(result);

    // When & Then
    mockMvc
        .perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.access_token").value("new-access-token"))
        .andExpect(jsonPath("$.refresh_token").value("new-refresh-token"));
  }

  @Test
  void testRefreshTokenWithInvalidToken() throws Exception {
    // Given
    AuthRequest request = new AuthRequest();
    request.setRefreshToken("invalid-refresh-token");
    when(authenticationService.refreshToken(anyString(), anyString()))
        .thenThrow(new ApiException("INVALID_REFRESH_TOKEN", "Invalid refresh token"));

    // When & Then
    mockMvc
        .perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error").value("INVALID_REFRESH_TOKEN"));
  }

  @Test
  void testLogoutSuccess() throws Exception {
    // Given
    User user = createTestUser();
    when(authenticationService.validateToken(anyString())).thenReturn(user);

    // When & Then
    mockMvc
        .perform(post("/api/auth/logout").header("Authorization", "Bearer valid-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("Logged out successfully"));
  }

  @Test
  void testLogoutWithInvalidToken() throws Exception {
    // Given
    doThrow(new ApiException("INVALID_TOKEN", "Invalid token"))
        .when(authenticationService)
        .logout(anyString());

    // When & Then
    mockMvc
        .perform(post("/api/auth/logout").header("Authorization", "Bearer invalid-token"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error").value("INVALID_TOKEN"));
  }

  @Test
  void testGetCurrentUserSuccess() throws Exception {
    // Given
    User user = createTestUser();
    when(authenticationService.validateToken(anyString())).thenReturn(user);

    // When & Then
    mockMvc
        .perform(get("/api/auth/me").header("Authorization", "Bearer valid-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(user.getId().toString()))
        .andExpect(jsonPath("$.name").value(user.getName()))
        .andExpect(jsonPath("$.phone").value(user.getPhone()));
  }

  @Test
  void testGetCurrentUserWithInvalidToken() throws Exception {
    // Given
    when(authenticationService.validateToken(anyString()))
        .thenThrow(new ApiException("INVALID_TOKEN", "Invalid token"));

    // When & Then
    mockMvc
        .perform(get("/api/auth/me").header("Authorization", "Bearer invalid-token"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error").value("INVALID_TOKEN"));
  }

  @Test
  void testGetCurrentUserWithoutAuthHeader() throws Exception {
    // When & Then
    mockMvc
        .perform(get("/api/auth/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error").value("MISSING_AUTH_HEADER"));
  }

  private User createTestUser() {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setName("Test User");
    user.setPhone("+14155552671");
    user.setEmail("test@example.com");
    user.setRole(Role.USER);
    user.setCreatedAt(Instant.now());
    user.setUpdatedAt(Instant.now());
    return user;
  }
}
