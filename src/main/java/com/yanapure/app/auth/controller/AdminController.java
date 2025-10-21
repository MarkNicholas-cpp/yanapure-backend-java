package com.yanapure.app.auth.controller;

import com.yanapure.app.auth.dto.AuthResponse;
import com.yanapure.app.auth.service.AdminService;
import com.yanapure.app.auth.service.AuthenticationService;
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
 * REST controller for admin operations
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "${app.cors.origins:http://localhost:5173}")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final AdminService adminService;
    private final AuthenticationService authenticationService;

    public AdminController(AdminService adminService, AuthenticationService authenticationService) {
        this.adminService = adminService;
        this.authenticationService = authenticationService;
    }

    /**
     * Create admin user
     */
    @PostMapping("/users")
    public ResponseEntity<?> createAdminUser(
            @RequestBody CreateAdminRequest request,
            @RequestHeader("Authorization") String authHeader) {

        log.info("Create admin user request");

        try {
            String token = extractTokenFromHeader(authHeader);
            User currentUser = authenticationService.validateToken(token);

            if (!currentUser.isAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "success", false,
                        "error", "INSUFFICIENT_PERMISSIONS",
                        "message", "Admin access required"));
            }

            User adminUser = adminService.createAdminUser(
                    request.getPhone(),
                    request.getName(),
                    request.getEmail());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Admin user created successfully",
                    "user", new AuthResponse.UserDto(adminUser)));
        } catch (ApiException e) {
            log.warn("Create admin user failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.code(),
                    "message", e.getMessage()));
        }
    }

    /**
     * Promote user to admin
     */
    @PutMapping("/users/{userId}/promote")
    public ResponseEntity<?> promoteToAdmin(
            @PathVariable UUID userId,
            @RequestHeader("Authorization") String authHeader) {

        log.info("Promote user to admin request: {}", userId);

        try {
            String token = extractTokenFromHeader(authHeader);
            User currentUser = authenticationService.validateToken(token);

            if (!currentUser.isAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "success", false,
                        "error", "INSUFFICIENT_PERMISSIONS",
                        "message", "Admin access required"));
            }

            User promotedUser = adminService.promoteToAdmin(userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "User promoted to admin successfully",
                    "user", new AuthResponse.UserDto(promotedUser)));
        } catch (ApiException e) {
            log.warn("Promote user to admin failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.code(),
                    "message", e.getMessage()));
        }
    }

    /**
     * Demote admin to user
     */
    @PutMapping("/users/{userId}/demote")
    public ResponseEntity<?> demoteFromAdmin(
            @PathVariable UUID userId,
            @RequestHeader("Authorization") String authHeader) {

        log.info("Demote admin to user request: {}", userId);

        try {
            String token = extractTokenFromHeader(authHeader);
            User currentUser = authenticationService.validateToken(token);

            if (!currentUser.isAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "success", false,
                        "error", "INSUFFICIENT_PERMISSIONS",
                        "message", "Admin access required"));
            }

            User demotedUser = adminService.demoteFromAdmin(userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Admin demoted to user successfully",
                    "user", new AuthResponse.UserDto(demotedUser)));
        } catch (ApiException e) {
            log.warn("Demote admin to user failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.code(),
                    "message", e.getMessage()));
        }
    }

    /**
     * Get all admin users
     */
    @GetMapping("/users")
    public ResponseEntity<?> getAllAdmins(
            @RequestHeader("Authorization") String authHeader) {

        log.info("Get all admin users request");

        try {
            String token = extractTokenFromHeader(authHeader);
            User currentUser = authenticationService.validateToken(token);

            if (!currentUser.isAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "success", false,
                        "error", "INSUFFICIENT_PERMISSIONS",
                        "message", "Admin access required"));
            }

            List<User> admins = adminService.getAllAdmins();
            List<AuthResponse.UserDto> adminDtos = admins.stream()
                    .map(AuthResponse.UserDto::new)
                    .toList();

            return ResponseEntity.ok(Map.of("admins", adminDtos));
        } catch (ApiException e) {
            log.warn("Get all admin users failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "error", e.code(),
                    "message", e.getMessage()));
        }
    }

    /**
     * Get admin statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getAdminStats(
            @RequestHeader("Authorization") String authHeader) {

        log.info("Get admin statistics request");

        try {
            String token = extractTokenFromHeader(authHeader);
            User currentUser = authenticationService.validateToken(token);

            if (!currentUser.isAdmin()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                        "success", false,
                        "error", "INSUFFICIENT_PERMISSIONS",
                        "message", "Admin access required"));
            }

            AdminService.AdminStats stats = adminService.getAdminStats();

            return ResponseEntity.ok(Map.of(
                    "totalUsers", stats.getTotalUsers(),
                    "adminCount", stats.getAdminCount(),
                    "userCount", stats.getUserCount(),
                    "usersWithEmail", stats.getUsersWithEmail(),
                    "recentUsers", stats.getRecentUsers()));
        } catch (ApiException e) {
            log.warn("Get admin statistics failed: {}", e.getMessage());
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

    /**
     * Request DTO for creating admin user
     */
    public static class CreateAdminRequest {
        private String phone;
        private String name;
        private String email;

        // Constructors
        public CreateAdminRequest() {
        }

        public CreateAdminRequest(String phone, String name, String email) {
            this.phone = phone;
            this.name = name;
            this.email = email;
        }

        // Getters and Setters
        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }
}
