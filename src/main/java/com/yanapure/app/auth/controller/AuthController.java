package com.yanapure.app.auth.controller;

import com.yanapure.app.auth.dto.AuthRequest;
import com.yanapure.app.auth.dto.AuthResponse;
import com.yanapure.app.auth.dto.UserSessionDto;
import com.yanapure.app.auth.service.AuthenticationService;
import com.yanapure.app.common.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** REST controller for authentication operations */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "${app.cors.origins:http://localhost:5173}")
public class AuthController {

  private static final Logger log = LoggerFactory.getLogger(AuthController.class);

  private final AuthenticationService authenticationService;

  @Value("${app.auth.access-token-expiry-hours:1}")
  private int accessTokenExpiryHours;

  public AuthController(AuthenticationService authenticationService) {
    this.authenticationService = authenticationService;
  }

  /** Send OTP to phone number */
  @PostMapping("/send-otp")
  public ResponseEntity<Map<String, Object>> sendOtp(
      @RequestBody AuthRequest request, HttpServletRequest httpRequest) {

    String clientIp = getClientIp(httpRequest);
    log.info("OTP request for phone: {}", maskPhone(request.getPhone()));

    try {
      // Validate phone number format
      if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
        throw new ApiException("PHONE_REQUIRED", "Phone number is required");
      }

      authenticationService.initiatePhoneAuth(request.getPhone(), clientIp);

      return ResponseEntity.ok(
          Map.of(
              "success",
              true,
              "message",
              "Verification code sent successfully",
              "phone",
              maskPhone(request.getPhone())));
    } catch (ApiException e) {
      log.warn("OTP send failed: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(
              Map.of(
                  "success", false,
                  "error", e.code(),
                  "message", e.getMessage()));
    }
  }

  /** Verify OTP and login */
  @PostMapping("/verify-otp")
  public ResponseEntity<?> verifyOtp(
      @RequestBody AuthRequest request, HttpServletRequest httpRequest) {

    String clientIp = getClientIp(httpRequest);
    String userAgent = httpRequest.getHeader("User-Agent");

    log.info("OTP verification for phone: {}", maskPhone(request.getPhone()));

    try {
      AuthenticationService.AuthResult result =
          authenticationService.verifyPhoneAndLogin(
              request.getPhone(), request.getOtp(), clientIp, userAgent);

      long expiresIn = accessTokenExpiryHours * 3600; // Convert to seconds

      AuthResponse response =
          new AuthResponse(
              result.getUser(),
              result.getAccessToken(),
              result.getRefreshToken(),
              expiresIn,
              result.getSessionId());

      return ResponseEntity.ok(response);
    } catch (ApiException e) {
      log.warn("OTP verification failed: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(
              Map.of(
                  "success", false,
                  "error", e.code(),
                  "message", e.getMessage()));
    }
  }

  /** Refresh access token */
  @PostMapping("/refresh")
  public ResponseEntity<?> refreshToken(
      @RequestBody AuthRequest request, HttpServletRequest httpRequest) {

    String clientIp = getClientIp(httpRequest);
    log.info("Token refresh request from IP: {}", clientIp);

    try {
      AuthenticationService.AuthResult result =
          authenticationService.refreshToken(request.getRefreshToken(), clientIp);

      long expiresIn = accessTokenExpiryHours * 3600; // Convert to seconds

      AuthResponse response =
          new AuthResponse(
              result.getUser(),
              result.getAccessToken(),
              result.getRefreshToken(),
              expiresIn,
              result.getSessionId());

      return ResponseEntity.ok(response);
    } catch (ApiException e) {
      log.warn("Token refresh failed: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(
              Map.of(
                  "success", false,
                  "error", e.code(),
                  "message", e.getMessage()));
    }
  }

  /** Logout (deactivate current session) */
  @PostMapping("/logout")
  public ResponseEntity<Map<String, Object>> logout(
      @RequestHeader(value = "Authorization", required = false) String authHeader) {

    log.info("Logout request");

    try {
      if (authHeader == null) {
        throw new ApiException("MISSING_AUTH_HEADER", "Authorization header is required");
      }

      String token = extractTokenFromHeader(authHeader);
      authenticationService.logout(token);

      return ResponseEntity.ok(Map.of("success", true, "message", "Logged out successfully"));
    } catch (ApiException e) {
      log.warn("Logout failed: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(
              Map.of(
                  "success", false,
                  "error", e.code(),
                  "message", e.getMessage()));
    }
  }

  /** Logout from all devices */
  @PostMapping("/logout-all")
  public ResponseEntity<Map<String, Object>> logoutAll(
      @RequestHeader("Authorization") String authHeader) {

    log.info("Logout all devices request");

    try {
      String token = extractTokenFromHeader(authHeader);
      var user = authenticationService.validateToken(token);
      authenticationService.logoutAllDevices(user.getId());

      return ResponseEntity.ok(
          Map.of("success", true, "message", "Logged out from all devices successfully"));
    } catch (ApiException e) {
      log.warn("Logout all failed: {}", e.getMessage());
      return ResponseEntity.badRequest()
          .body(
              Map.of(
                  "success", false,
                  "error", e.code(),
                  "message", e.getMessage()));
    }
  }

  /** Get current user info */
  @GetMapping("/me")
  public ResponseEntity<?> getCurrentUser(
      @RequestHeader(value = "Authorization", required = false) String authHeader) {

    log.info("Get current user request");

    try {
      if (authHeader == null) {
        throw new ApiException("MISSING_AUTH_HEADER", "Authorization header is required");
      }

      String token = extractTokenFromHeader(authHeader);
      var user = authenticationService.validateToken(token);

      return ResponseEntity.ok(new AuthResponse.UserDto(user));
    } catch (ApiException e) {
      log.warn("Get current user failed: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(
              Map.of(
                  "success", false,
                  "error", e.code(),
                  "message", e.getMessage()));
    }
  }

  /** Get user sessions */
  @GetMapping("/sessions")
  public ResponseEntity<?> getUserSessions(@RequestHeader("Authorization") String authHeader) {

    log.info("Get user sessions request");

    try {
      String token = extractTokenFromHeader(authHeader);
      var user = authenticationService.validateToken(token);
      var sessions = authenticationService.getUserSessions(user.getId());

      return ResponseEntity.ok(
          Map.of("sessions", sessions.stream().map(UserSessionDto::new).toList()));
    } catch (ApiException e) {
      log.warn("Get user sessions failed: {}", e.getMessage());
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(
              Map.of(
                  "success", false,
                  "error", e.code(),
                  "message", e.getMessage()));
    }
  }

  /** Extract client IP from request */
  private String getClientIp(HttpServletRequest request) {
    String xForwardedFor = request.getHeader("X-Forwarded-For");
    if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
      return xForwardedFor.split(",")[0].trim();
    }

    String xRealIp = request.getHeader("X-Real-IP");
    if (xRealIp != null && !xRealIp.isEmpty()) {
      return xRealIp;
    }

    return request.getRemoteAddr();
  }

  /** Extract token from Authorization header */
  private String extractTokenFromHeader(String authHeader) {
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      throw new ApiException("INVALID_AUTH_HEADER", "Invalid authorization header");
    }
    return authHeader.substring(7);
  }

  /** Mask phone number for logging */
  private String maskPhone(String phone) {
    if (phone == null || phone.length() < 4) {
      return "***";
    }
    return phone.substring(0, 2)
        + "*".repeat(phone.length() - 4)
        + phone.substring(phone.length() - 2);
  }
}
