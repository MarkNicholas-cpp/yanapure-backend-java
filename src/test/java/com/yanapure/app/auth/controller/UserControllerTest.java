package com.yanapure.app.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yanapure.app.auth.dto.UserUpdateRequest;
import com.yanapure.app.auth.service.AuthenticationService;
import com.yanapure.app.auth.service.UserService;
import com.yanapure.app.common.ApiException;
import com.yanapure.app.users.Role;
import com.yanapure.app.users.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private AuthenticationService authenticationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testGetCurrentUserSuccess() throws Exception {
        // Given
        User user = createTestUser();
        when(authenticationService.validateToken(anyString())).thenReturn(user);

        // When & Then
        mockMvc.perform(get("/api/users/me")
                .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.name").value(user.getName()))
                .andExpect(jsonPath("$.phone").value(user.getPhone()));
    }

    @Test
    void testUpdateCurrentUserSuccess() throws Exception {
        // Given
        User currentUser = createTestUser();
        User updatedUser = createTestUser();
        updatedUser.setName("Updated Name");
        updatedUser.setEmail("updated@example.com");

        UserUpdateRequest request = new UserUpdateRequest("Updated Name", "updated@example.com");

        when(authenticationService.validateToken(anyString())).thenReturn(currentUser);
        when(userService.updateUserProfile(any(UUID.class), anyString(), anyString())).thenReturn(updatedUser);

        // When & Then
        mockMvc.perform(put("/api/users/me")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.email").value("updated@example.com"));
    }

    @Test
    void testUpdateCurrentUserWithInvalidEmail() throws Exception {
        // Given
        User currentUser = createTestUser();
        when(authenticationService.validateToken(anyString())).thenReturn(currentUser);
        when(userService.updateUserProfile(any(UUID.class), anyString(), anyString()))
                .thenThrow(new ApiException("INVALID_EMAIL", "Invalid email format"));

        // When & Then
        mockMvc.perform(put("/api/users/me")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                            {"name": "Updated Name", "email": "invalid-email"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("INVALID_EMAIL"))
                .andExpect(jsonPath("$.message").value("Invalid email format"));
    }

    @Test
    void testGetAllUsersAsAdmin() throws Exception {
        // Given
        User adminUser = createTestUser();
        adminUser.setRole(Role.ADMIN);
        List<User> users = List.of(createTestUser(), createTestUser());

        when(authenticationService.validateToken(anyString())).thenReturn(adminUser);
        when(userService.getAllUsers()).thenReturn(users);

        // When & Then
        mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users").isArray())
                .andExpect(jsonPath("$.users.length()").value(2));
    }

    @Test
    void testGetAllUsersAsRegularUser() throws Exception {
        // Given
        User regularUser = createTestUser();
        regularUser.setRole(Role.USER);

        when(authenticationService.validateToken(anyString())).thenReturn(regularUser);

        // When & Then
        mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_PERMISSIONS"));
    }

    @Test
    void testGetUserByIdAsAdmin() throws Exception {
        // Given
        User adminUser = createTestUser();
        adminUser.setRole(Role.ADMIN);
        User targetUser = createTestUser();
        targetUser.setId(UUID.randomUUID());

        when(authenticationService.validateToken(anyString())).thenReturn(adminUser);
        when(userService.getUserById(any(UUID.class))).thenReturn(targetUser);

        // When & Then
        mockMvc.perform(get("/api/users/" + targetUser.getId())
                .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(targetUser.getId().toString()));
    }

    @Test
    void testUpdateUserRoleAsAdmin() throws Exception {
        // Given
        User adminUser = createTestUser();
        adminUser.setRole(Role.ADMIN);
        User targetUser = createTestUser();
        targetUser.setRole(Role.ADMIN);

        when(authenticationService.validateToken(anyString())).thenReturn(adminUser);
        when(userService.updateUserRole(any(UUID.class), any(Role.class))).thenReturn(targetUser);

        // When & Then
        mockMvc.perform(put("/api/users/" + targetUser.getId() + "/role")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\": \"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void testUpdateUserRoleWithInvalidRole() throws Exception {
        // Given
        User adminUser = createTestUser();
        adminUser.setRole(Role.ADMIN);
        User targetUser = createTestUser();

        when(authenticationService.validateToken(anyString())).thenReturn(adminUser);

        // When & Then
        mockMvc.perform(put("/api/users/" + targetUser.getId() + "/role")
                .header("Authorization", "Bearer valid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\": \"INVALID_ROLE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("INVALID_ROLE"));
    }

    @Test
    void testDeleteUserAsAdmin() throws Exception {
        // Given
        User adminUser = createTestUser();
        adminUser.setRole(Role.ADMIN);
        User targetUser = createTestUser();
        targetUser.setId(UUID.randomUUID());

        when(authenticationService.validateToken(anyString())).thenReturn(adminUser);

        // When & Then
        mockMvc.perform(delete("/api/users/" + targetUser.getId())
                .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User deleted successfully"));
    }

    @Test
    void testDeleteSelf() throws Exception {
        // Given
        User adminUser = createTestUser();
        adminUser.setRole(Role.ADMIN);

        when(authenticationService.validateToken(anyString())).thenReturn(adminUser);

        // When & Then
        mockMvc.perform(delete("/api/users/" + adminUser.getId())
                .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("CANNOT_DELETE_SELF"));
    }

    @Test
    void testGetUserStatsAsAdmin() throws Exception {
        // Given
        User adminUser = createTestUser();
        adminUser.setRole(Role.ADMIN);
        UserService.UserStats stats = new UserService.UserStats(100, 5, 95, 80);

        when(authenticationService.validateToken(anyString())).thenReturn(adminUser);
        when(userService.getUserStats()).thenReturn(stats);

        // When & Then
        mockMvc.perform(get("/api/users/stats")
                .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(100))
                .andExpect(jsonPath("$.adminCount").value(5))
                .andExpect(jsonPath("$.userCount").value(95))
                .andExpect(jsonPath("$.usersWithEmail").value(80));
    }

    @Test
    void testGetUserStatsAsRegularUser() throws Exception {
        // Given
        User regularUser = createTestUser();
        regularUser.setRole(Role.USER);

        when(authenticationService.validateToken(anyString())).thenReturn(regularUser);

        // When & Then
        mockMvc.perform(get("/api/users/stats")
                .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_PERMISSIONS"));
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
