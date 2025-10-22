package com.yanapure.app.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanapure.app.auth.service.AdminService;
import com.yanapure.app.auth.service.AuthenticationService;
import com.yanapure.app.common.ApiException;
import com.yanapure.app.common.GlobalExceptionHandler;
import com.yanapure.app.users.Role;
import com.yanapure.app.users.User;
import java.time.Instant;
import java.util.List;
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

@WebMvcTest(controllers = AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@TestPropertySource(
    properties = {
      "app.jwt.secret=test-secret-key-for-testing-only",
      "app.jwt.access-token-expiry-hours=1",
      "app.jwt.refresh-token-expiry-days=7",
      "app.auth.access-token-expiry-hours=1"
    })
public class AdminControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private AdminService adminService;

  @MockBean private AuthenticationService authenticationService;

  @Autowired private ObjectMapper objectMapper;

  @Test
  void testCreateAdminUserSuccess() throws Exception {
    // Given
    User adminUser = createTestAdminUser();
    when(authenticationService.validateToken(anyString())).thenReturn(adminUser);
    when(adminService.createAdminUser(anyString(), anyString(), anyString())).thenReturn(adminUser);

    // When & Then
    mockMvc
        .perform(
            post("/api/admin/users")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                            {"phone": "+14155550001", "name": "New Admin", "email": "newadmin@example.com"}
                        """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("Admin user created successfully"))
        .andExpect(jsonPath("$.user.id").value(adminUser.getId().toString()))
        .andExpect(jsonPath("$.user.role").value("ADMIN"));
  }

  @Test
  void testCreateAdminUserWithInsufficientPermissions() throws Exception {
    // Given
    User regularUser = createTestUser();
    when(authenticationService.validateToken(anyString())).thenReturn(regularUser);

    // When & Then
    mockMvc
        .perform(
            post("/api/admin/users")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                            {"phone": "+14155550001", "name": "New Admin", "email": "newadmin@example.com"}
                        """))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error").value("INSUFFICIENT_PERMISSIONS"))
        .andExpect(jsonPath("$.message").value("Admin access required"));
  }

  @Test
  void testCreateAdminUserWithApiException() throws Exception {
    // Given
    User adminUser = createTestAdminUser();
    when(authenticationService.validateToken(anyString())).thenReturn(adminUser);
    when(adminService.createAdminUser(anyString(), anyString(), anyString()))
        .thenThrow(
            new ApiException("USER_ALREADY_EXISTS", "User with this phone number already exists"));

    // When & Then
    mockMvc
        .perform(
            post("/api/admin/users")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                            {"phone": "+14155550001", "name": "New Admin", "email": "newadmin@example.com"}
                        """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error").value("USER_ALREADY_EXISTS"))
        .andExpect(jsonPath("$.message").value("User with this phone number already exists"));
  }

  @Test
  void testPromoteToAdminSuccess() throws Exception {
    // Given
    User adminUser = createTestAdminUser();
    User promotedUser = createTestUser();
    promotedUser.setRole(Role.ADMIN);

    when(authenticationService.validateToken(anyString())).thenReturn(adminUser);
    when(adminService.promoteToAdmin(any(UUID.class))).thenReturn(promotedUser);

    // When & Then
    mockMvc
        .perform(
            put("/api/admin/users/{userId}/promote", promotedUser.getId())
                .header("Authorization", "Bearer valid-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("User promoted to admin successfully"))
        .andExpect(jsonPath("$.user.role").value("ADMIN"));
  }

  @Test
  void testDemoteFromAdminSuccess() throws Exception {
    // Given
    User adminUser = createTestAdminUser();
    User demotedUser = createTestAdminUser();
    demotedUser.setRole(Role.USER);

    when(authenticationService.validateToken(anyString())).thenReturn(adminUser);
    when(adminService.demoteFromAdmin(any(UUID.class))).thenReturn(demotedUser);

    // When & Then
    mockMvc
        .perform(
            put("/api/admin/users/{userId}/demote", demotedUser.getId())
                .header("Authorization", "Bearer valid-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.message").value("Admin demoted to user successfully"))
        .andExpect(jsonPath("$.user.role").value("USER"));
  }

  @Test
  void testGetAllAdminsSuccess() throws Exception {
    // Given
    User adminUser = createTestAdminUser();
    List<User> admins = List.of(adminUser, createTestAdminUser());

    when(authenticationService.validateToken(anyString())).thenReturn(adminUser);
    when(adminService.getAllAdmins()).thenReturn(admins);

    // When & Then
    mockMvc
        .perform(get("/api/admin/users").header("Authorization", "Bearer valid-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.admins").isArray())
        .andExpect(jsonPath("$.admins.length()").value(2))
        .andExpect(jsonPath("$.admins[0].role").value("ADMIN"));
  }

  @Test
  void testGetAdminStatsSuccess() throws Exception {
    // Given
    User adminUser = createTestAdminUser();
    AdminService.AdminStats stats = new AdminService.AdminStats(100, 5, 95, 80, 10);

    when(authenticationService.validateToken(anyString())).thenReturn(adminUser);
    when(adminService.getAdminStats()).thenReturn(stats);

    // When & Then
    mockMvc
        .perform(get("/api/admin/stats").header("Authorization", "Bearer valid-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalUsers").value(100))
        .andExpect(jsonPath("$.adminCount").value(5))
        .andExpect(jsonPath("$.userCount").value(95))
        .andExpect(jsonPath("$.usersWithEmail").value(80))
        .andExpect(jsonPath("$.recentUsers").value(10));
  }

  @Test
  void testGetAdminStatsWithInsufficientPermissions() throws Exception {
    // Given
    User regularUser = createTestUser();
    when(authenticationService.validateToken(anyString())).thenReturn(regularUser);

    // When & Then
    mockMvc
        .perform(get("/api/admin/stats").header("Authorization", "Bearer valid-token"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error").value("INSUFFICIENT_PERMISSIONS"))
        .andExpect(jsonPath("$.message").value("Admin access required"));
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

  private User createTestAdminUser() {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setName("Test Admin");
    user.setPhone("+14155550000");
    user.setEmail("admin@example.com");
    user.setRole(Role.ADMIN);
    user.setCreatedAt(Instant.now());
    user.setUpdatedAt(Instant.now());
    return user;
  }
}
