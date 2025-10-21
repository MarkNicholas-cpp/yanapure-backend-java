package com.yanapure.app.auth.controller;

import com.yanapure.app.auth.dto.AuthResponse;
import com.yanapure.app.auth.dto.UserUpdateRequest;
import com.yanapure.app.auth.service.AuthenticationService;
import com.yanapure.app.auth.service.UserService;
import com.yanapure.app.common.ApiException;
import com.yanapure.app.users.Role;
import com.yanapure.app.users.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for user management operations
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "${app.cors.origins:http://localhost:5173}")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final AuthenticationService authenticationService;

    public UserController(UserService userService, AuthenticationService authenticationService) {
        this.userService = userService;
        this.authenticationService = authenticationService;
    }

    /**
     * Get current user profile
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(
            @RequestHeader("Authorization") String authHeader) {

        log.info("Get current user profile request");

        try {
            String token = extractTokenFromHeader(authHeader);
            User user = authenticationService.validateToken(token);

            return ResponseEntity.ok(new AuthResponse.UserDto(user));
        } catch (ApiException e) {
            log.warn("Get current user failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "error", e.code(),
                    "message", e.getMessage()));
        }
    }

    /**
     * Update current user profile
     */
    @PutMapping("/me")
    public ResponseEntity<?> updateCurrentUser(
            @RequestBody UserUpdateRequest request,
            @RequestHeader("Authorization") String authHeader) {

        log.info("Update current user profile request");

        try {
            String token = extractTokenFromHeader(authHeader);
            User currentUser = authenticationService.validateToken(token);

            User updatedUser = userService.updateUserProfile(
                    currentUser.getId(),
                    request.getName(),
                    request.getEmail());

            return ResponseEntity.ok(new AuthResponse.UserDto(updatedUser));
        } catch (ApiException e) {
            log.warn("Update current user failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.code(),
                    "message", e.getMessage()));
        }
    }

    /**
     * Get all users (admin only)
     */
    @GetMapping
    public ResponseEntity<?> getAllUsers(
            @RequestHeader("Authorization") String authHeader) {

        log.info("Get all users request");

        try {
            String token = extractTokenFromHeader(authHeader);
            User currentUser = authenticationService.validateToken(token);

            if (!currentUser.isAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "success", false,
                        "error", "INSUFFICIENT_PERMISSIONS",
                        "message", "Admin access required"));
            }

            List<User> users = userService.getAllUsers();
            List<AuthResponse.UserDto> userDtos = users.stream()
                    .map(AuthResponse.UserDto::new)
                    .toList();

            return ResponseEntity.ok(Map.of("users", userDtos));
        } catch (ApiException e) {
            log.warn("Get all users failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "error", e.code(),
                    "message", e.getMessage()));
        }
    }

    /**
     * Get user by ID (admin only)
     */
    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserById(
            @PathVariable UUID userId,
            @RequestHeader("Authorization") String authHeader) {

        log.info("Get user by ID request: {}", userId);

        try {
            String token = extractTokenFromHeader(authHeader);
            User currentUser = authenticationService.validateToken(token);

            if (!currentUser.isAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "success", false,
                        "error", "INSUFFICIENT_PERMISSIONS",
                        "message", "Admin access required"));
            }

            User user = userService.getUserById(userId);
            return ResponseEntity.ok(new AuthResponse.UserDto(user));
        } catch (ApiException e) {
            log.warn("Get user by ID failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.code(),
                    "message", e.getMessage()));
        }
    }

    /**
     * Update user role (admin only)
     */
    @PutMapping("/{userId}/role")
    public ResponseEntity<?> updateUserRole(
            @PathVariable UUID userId,
            @RequestBody Map<String, String> request,
            @RequestHeader("Authorization") String authHeader) {

        log.info("Update user role request: {} -> {}", userId, request.get("role"));

        try {
            String token = extractTokenFromHeader(authHeader);
            User currentUser = authenticationService.validateToken(token);

            if (!currentUser.isAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "success", false,
                        "error", "INSUFFICIENT_PERMISSIONS",
                        "message", "Admin access required"));
            }

            String roleStr = request.get("role");
            if (roleStr == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "MISSING_ROLE",
                        "message", "Role is required"));
            }

            Role newRole;
            try {
                newRole = Role.valueOf(roleStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "INVALID_ROLE",
                        "message", "Invalid role: " + roleStr));
            }

            User updatedUser = userService.updateUserRole(userId, newRole);
            return ResponseEntity.ok(new AuthResponse.UserDto(updatedUser));
        } catch (ApiException e) {
            log.warn("Update user role failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.code(),
                    "message", e.getMessage()));
        }
    }

    /**
     * Delete user (admin only)
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUser(
            @PathVariable UUID userId,
            @RequestHeader("Authorization") String authHeader) {

        log.info("Delete user request: {}", userId);

        try {
            String token = extractTokenFromHeader(authHeader);
            User currentUser = authenticationService.validateToken(token);

            if (!currentUser.isAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "success", false,
                        "error", "INSUFFICIENT_PERMISSIONS",
                        "message", "Admin access required"));
            }

            // Prevent self-deletion
            if (currentUser.getId().equals(userId)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "CANNOT_DELETE_SELF",
                        "message", "Cannot delete your own account"));
            }

            userService.deleteUser(userId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "User deleted successfully"));
        } catch (ApiException e) {
            log.warn("Delete user failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.code(),
                    "message", e.getMessage()));
        }
    }

    /**
     * Get user statistics (admin only)
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getUserStats(
            @RequestHeader("Authorization") String authHeader) {

        log.info("Get user statistics request");

        try {
            String token = extractTokenFromHeader(authHeader);
            User currentUser = authenticationService.validateToken(token);

            if (!currentUser.isAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "success", false,
                        "error", "INSUFFICIENT_PERMISSIONS",
                        "message", "Admin access required"));
            }

            UserService.UserStats stats = userService.getUserStats();
            return ResponseEntity.ok(Map.of(
                    "totalUsers", stats.getTotalUsers(),
                    "adminCount", stats.getAdminCount(),
                    "userCount", stats.getUserCount(),
                    "usersWithEmail", stats.getUsersWithEmail()));
        } catch (ApiException e) {
            log.warn("Get user stats failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "error", e.code(),
                    "message", e.getMessage()));
        }
    }

    /**
     * Extract token from Authorization header
     */
    private String extractTokenFromHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ApiException("INVALID_AUTH_HEADER", "Invalid authorization header");
        }
        return authHeader.substring(7);
    }
}
