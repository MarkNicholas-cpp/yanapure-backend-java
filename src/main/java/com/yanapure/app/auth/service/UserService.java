package com.yanapure.app.auth.service;

import com.yanapure.app.common.ApiException;
import com.yanapure.app.users.Role;
import com.yanapure.app.users.User;
import com.yanapure.app.users.UserRepository;
import com.yanapure.app.util.PhoneUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for user management operations
 */
@Service
@Transactional
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Get user by ID
     */
    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("USER_NOT_FOUND", "User not found"));
    }

    /**
     * Get user by phone number
     */
    public User getUserByPhone(String phoneNumber) {
        String normalizedPhone = PhoneUtils.normalizeToE164(phoneNumber);
        return userRepository.findByPhone(normalizedPhone)
                .orElseThrow(() -> new ApiException("USER_NOT_FOUND", "User not found"));
    }

    /**
     * Get user by email
     */
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("USER_NOT_FOUND", "User not found"));
    }

    /**
     * Update user profile
     */
    public User updateUserProfile(UUID userId, String name, String email) {
        User user = getUserById(userId);

        if (name != null && !name.trim().isEmpty()) {
            user.setName(name.trim());
        }

        if (email != null && !email.trim().isEmpty()) {
            // Check if email is already taken by another user
            if (userRepository.existsByEmail(email) && !email.equals(user.getEmail())) {
                throw new ApiException("EMAIL_ALREADY_EXISTS", "Email is already taken");
            }
            user.setEmail(email.trim());
        }

        user.setUpdatedAt(Instant.now());
        return userRepository.save(user);
    }

    /**
     * Update user role (admin only)
     */
    public User updateUserRole(UUID userId, Role newRole) {
        User user = getUserById(userId);
        user.setRole(newRole);
        user.setUpdatedAt(Instant.now());

        log.info("Updated user role: {} -> {} (User ID: {})",
                user.getRole(), newRole, userId);

        return userRepository.save(user);
    }

    /**
     * Get all users (admin only)
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Get users by role
     */
    public List<User> getUsersByRole(Role role) {
        return userRepository.findByRole(role);
    }

    /**
     * Get user statistics
     */
    public UserStats getUserStats() {
        long totalUsers = userRepository.count();
        long adminCount = userRepository.countByRole(Role.ADMIN);
        long userCount = userRepository.countByRole(Role.USER);
        long usersWithEmail = userRepository.findUsersWithEmail().size();

        return new UserStats(totalUsers, adminCount, userCount, usersWithEmail);
    }

    /**
     * Check if user exists by phone
     */
    public boolean userExistsByPhone(String phoneNumber) {
        String normalizedPhone = PhoneUtils.normalizeToE164(phoneNumber);
        return userRepository.existsByPhone(normalizedPhone);
    }

    /**
     * Check if user exists by email
     */
    public boolean userExistsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Get users created after a specific date
     */
    public List<User> getUsersCreatedAfter(Instant createdAt) {
        return userRepository.findByCreatedAtAfter(createdAt);
    }

    /**
     * Delete user (admin only)
     */
    public void deleteUser(UUID userId) {
        User user = getUserById(userId);

        // Prevent deleting the last admin
        if (user.getRole() == Role.ADMIN) {
            long adminCount = userRepository.countByRole(Role.ADMIN);
            if (adminCount <= 1) {
                throw new ApiException("CANNOT_DELETE_LAST_ADMIN",
                        "Cannot delete the last admin user");
            }
        }

        userRepository.delete(user);
        log.info("Deleted user: {} (ID: {})", PhoneUtils.maskPhone(user.getPhone()), userId);
    }

    /**
     * User statistics DTO
     */
    public static class UserStats {
        private final long totalUsers;
        private final long adminCount;
        private final long userCount;
        private final long usersWithEmail;

        public UserStats(long totalUsers, long adminCount, long userCount, long usersWithEmail) {
            this.totalUsers = totalUsers;
            this.adminCount = adminCount;
            this.userCount = userCount;
            this.usersWithEmail = usersWithEmail;
        }

        public long getTotalUsers() {
            return totalUsers;
        }

        public long getAdminCount() {
            return adminCount;
        }

        public long getUserCount() {
            return userCount;
        }

        public long getUsersWithEmail() {
            return usersWithEmail;
        }
    }
}
