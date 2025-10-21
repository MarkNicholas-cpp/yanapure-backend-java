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
 * Service for admin-specific operations
 */
@Service
@Transactional
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final UserRepository userRepository;
    private final UserService userService;

    public AdminService(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    /**
     * Create admin user
     */
    public User createAdminUser(String phoneNumber, String name, String email) {
        String normalizedPhone = PhoneUtils.normalizeToE164(phoneNumber);

        // Check if user already exists
        if (userRepository.existsByPhone(normalizedPhone)) {
            throw new ApiException("USER_ALREADY_EXISTS", "User with this phone number already exists");
        }

        // Check if email is provided and not already taken
        if (email != null && !email.trim().isEmpty()) {
            if (userRepository.existsByEmail(email)) {
                throw new ApiException("EMAIL_ALREADY_EXISTS", "Email already exists");
            }
        }

        User adminUser = new User();
        adminUser.setName(name);
        adminUser.setPhone(normalizedPhone);
        adminUser.setEmail(email);
        adminUser.setRole(Role.ADMIN);
        adminUser.setCreatedAt(Instant.now());
        adminUser.setUpdatedAt(Instant.now());

        User savedAdmin = userRepository.save(adminUser);
        log.info("Created admin user: {} (Phone: {})", name, PhoneUtils.maskPhone(normalizedPhone));

        return savedAdmin;
    }

    /**
     * Promote user to admin
     */
    public User promoteToAdmin(UUID userId) {
        User user = userService.getUserById(userId);

        if (user.isAdmin()) {
            throw new ApiException("ALREADY_ADMIN", "User is already an admin");
        }

        user.setRole(Role.ADMIN);
        user.setUpdatedAt(Instant.now());

        User updatedUser = userRepository.save(user);
        log.info("Promoted user to admin: {} (ID: {})", user.getName(), userId);

        return updatedUser;
    }

    /**
     * Demote admin to user
     */
    public User demoteFromAdmin(UUID userId) {
        User user = userService.getUserById(userId);

        if (!user.isAdmin()) {
            throw new ApiException("NOT_ADMIN", "User is not an admin");
        }

        // Prevent demoting the last admin
        long adminCount = userRepository.countByRole(Role.ADMIN);
        if (adminCount <= 1) {
            throw new ApiException("LAST_ADMIN", "Cannot demote the last admin user");
        }

        user.setRole(Role.USER);
        user.setUpdatedAt(Instant.now());

        User updatedUser = userRepository.save(user);
        log.info("Demoted admin to user: {} (ID: {})", user.getName(), userId);

        return updatedUser;
    }

    /**
     * Get all admin users
     */
    public List<User> getAllAdmins() {
        return userService.getUsersByRole(Role.ADMIN);
    }

    /**
     * Get admin statistics
     */
    public AdminStats getAdminStats() {
        long totalUsers = userRepository.count();
        long adminCount = userRepository.countByRole(Role.ADMIN);
        long userCount = userRepository.countByRole(Role.USER);
        long usersWithEmail = userRepository.findUsersWithEmail().size();
        long recentUsers = userRepository.countByCreatedAtAfter(Instant.now().minusSeconds(86400 * 7)); // Last 7 days

        return new AdminStats(totalUsers, adminCount, userCount, usersWithEmail, recentUsers);
    }

    /**
     * Admin statistics DTO
     */
    public static class AdminStats {
        private final long totalUsers;
        private final long adminCount;
        private final long userCount;
        private final long usersWithEmail;
        private final long recentUsers;

        public AdminStats(long totalUsers, long adminCount, long userCount, long usersWithEmail, long recentUsers) {
            this.totalUsers = totalUsers;
            this.adminCount = adminCount;
            this.userCount = userCount;
            this.usersWithEmail = usersWithEmail;
            this.recentUsers = recentUsers;
        }

        // Getters
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

        public long getRecentUsers() {
            return recentUsers;
        }
    }
}
