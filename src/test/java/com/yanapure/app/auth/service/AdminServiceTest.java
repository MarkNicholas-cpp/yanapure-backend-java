package com.yanapure.app.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.yanapure.app.common.ApiException;
import com.yanapure.app.users.Role;
import com.yanapure.app.users.User;
import com.yanapure.app.users.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

  @Mock private UserRepository userRepository;

  @Mock private UserService userService;

  @InjectMocks private AdminService adminService;

  private User testUser;
  private User testAdmin;

  @BeforeEach
  void setUp() {
    testUser = createTestUser();
    testAdmin = createTestAdmin();
  }

  @Test
  void testCreateAdminUserSuccess() {
    // Given
    String phone = "+14155550001";
    String name = "New Admin";
    String email = "newadmin@example.com";

    User expectedAdmin = new User();
    expectedAdmin.setId(UUID.randomUUID());
    expectedAdmin.setName(name);
    expectedAdmin.setPhone(phone);
    expectedAdmin.setEmail(email);
    expectedAdmin.setRole(Role.ADMIN);
    expectedAdmin.setCreatedAt(Instant.now());
    expectedAdmin.setUpdatedAt(Instant.now());

    when(userRepository.existsByPhone(anyString())).thenReturn(false);
    when(userRepository.existsByEmail(anyString())).thenReturn(false);
    when(userRepository.save(any(User.class))).thenReturn(expectedAdmin);

    // When
    User result = adminService.createAdminUser(phone, name, email);

    // Then
    assertNotNull(result);
    assertEquals(Role.ADMIN, result.getRole());
    assertEquals(name, result.getName());
    verify(userRepository).save(any(User.class));
  }

  @Test
  void testCreateAdminUserWithExistingPhone() {
    // Given
    String phone = "+14155550001";
    String name = "New Admin";
    String email = "newadmin@example.com";

    when(userRepository.existsByPhone(anyString())).thenReturn(true);

    // When & Then
    ApiException exception =
        assertThrows(
            ApiException.class,
            () -> {
              adminService.createAdminUser(phone, name, email);
            });

    assertEquals("USER_ALREADY_EXISTS", exception.code());
    assertEquals("User with this phone number already exists", exception.getMessage());
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void testCreateAdminUserWithExistingEmail() {
    // Given
    String phone = "+14155550001";
    String name = "New Admin";
    String email = "existing@example.com";

    when(userRepository.existsByPhone(anyString())).thenReturn(false);
    when(userRepository.existsByEmail(anyString())).thenReturn(true);

    // When & Then
    ApiException exception =
        assertThrows(
            ApiException.class,
            () -> {
              adminService.createAdminUser(phone, name, email);
            });

    assertEquals("EMAIL_ALREADY_EXISTS", exception.code());
    assertEquals("Email already exists", exception.getMessage());
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void testPromoteToAdminSuccess() {
    // Given
    UUID userId = testUser.getId();
    when(userService.getUserById(userId)).thenReturn(testUser);
    when(userRepository.save(any(User.class))).thenReturn(testUser);

    // When
    User result = adminService.promoteToAdmin(userId);

    // Then
    assertNotNull(result);
    assertEquals(Role.ADMIN, result.getRole());
    verify(userRepository).save(testUser);
  }

  @Test
  void testPromoteToAdminAlreadyAdmin() {
    // Given
    UUID userId = testAdmin.getId();
    when(userService.getUserById(userId)).thenReturn(testAdmin);

    // When & Then
    ApiException exception =
        assertThrows(
            ApiException.class,
            () -> {
              adminService.promoteToAdmin(userId);
            });

    assertEquals("ALREADY_ADMIN", exception.code());
    assertEquals("User is already an admin", exception.getMessage());
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void testDemoteFromAdminSuccess() {
    // Given
    UUID userId = testAdmin.getId();
    when(userService.getUserById(userId)).thenReturn(testAdmin);
    when(userRepository.countByRole(Role.ADMIN)).thenReturn(2L);
    when(userRepository.save(any(User.class))).thenReturn(testAdmin);

    // When
    User result = adminService.demoteFromAdmin(userId);

    // Then
    assertNotNull(result);
    assertEquals(Role.USER, result.getRole());
    verify(userRepository).save(testAdmin);
  }

  @Test
  void testDemoteFromAdminNotAdmin() {
    // Given
    UUID userId = testUser.getId();
    when(userService.getUserById(userId)).thenReturn(testUser);

    // When & Then
    ApiException exception =
        assertThrows(
            ApiException.class,
            () -> {
              adminService.demoteFromAdmin(userId);
            });

    assertEquals("NOT_ADMIN", exception.code());
    assertEquals("User is not an admin", exception.getMessage());
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void testDemoteFromAdminLastAdmin() {
    // Given
    UUID userId = testAdmin.getId();
    when(userService.getUserById(userId)).thenReturn(testAdmin);
    when(userRepository.countByRole(Role.ADMIN)).thenReturn(1L);

    // When & Then
    ApiException exception =
        assertThrows(
            ApiException.class,
            () -> {
              adminService.demoteFromAdmin(userId);
            });

    assertEquals("LAST_ADMIN", exception.code());
    assertEquals("Cannot demote the last admin user", exception.getMessage());
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void testGetAllAdmins() {
    // Given
    List<User> admins = List.of(testAdmin);
    when(userService.getUsersByRole(Role.ADMIN)).thenReturn(admins);

    // When
    List<User> result = adminService.getAllAdmins();

    // Then
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(Role.ADMIN, result.get(0).getRole());
  }

  @Test
  void testGetAdminStats() {
    // Given
    when(userRepository.count()).thenReturn(100L);
    when(userRepository.countByRole(Role.ADMIN)).thenReturn(5L);
    when(userRepository.countByRole(Role.USER)).thenReturn(95L);
    when(userRepository.findUsersWithEmail()).thenReturn(List.of(new User(), new User()));
    when(userRepository.countByCreatedAtAfter(any(Instant.class))).thenReturn(10L);

    // When
    AdminService.AdminStats stats = adminService.getAdminStats();

    // Then
    assertNotNull(stats);
    assertEquals(100L, stats.getTotalUsers());
    assertEquals(5L, stats.getAdminCount());
    assertEquals(95L, stats.getUserCount());
    assertEquals(2L, stats.getUsersWithEmail());
    assertEquals(10L, stats.getRecentUsers());
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

  private User createTestAdmin() {
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
