package com.yanapure.app.config;

import com.yanapure.app.users.Role;
import com.yanapure.app.users.User;
import com.yanapure.app.users.UserRepository;
import com.yanapure.app.util.PhoneUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Seeds initial admin user on application startup
 */
@Component
public class AdminUserSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserSeeder.class);

    private final UserRepository userRepository;
    private final Environment environment;

    public AdminUserSeeder(UserRepository userRepository, Environment environment) {
        this.userRepository = userRepository;
        this.environment = environment;
    }

    @Override
    public void run(String... args) throws Exception {
        seedAdminUser();
    }

    private void seedAdminUser() {
        try {
            // Check if any admin users already exist
            long adminCount = userRepository.countByRole(Role.ADMIN);
            if (adminCount > 0) {
                log.info("Admin users already exist (count: {}), skipping seed", adminCount);
                return;
            }

            // Get admin user details from environment variables
            String adminPhone = environment.getProperty("app.admin.phone", "+14155550000");
            String adminName = environment.getProperty("app.admin.name", "System Admin");
            String adminEmail = environment.getProperty("app.admin.email", "admin@yanapure.com");

            // Normalize phone number
            String normalizedPhone = PhoneUtils.normalizeToE164(adminPhone);

            // Check if user with this phone already exists
            if (userRepository.existsByPhone(normalizedPhone)) {
                log.warn("User with admin phone {} already exists, skipping seed",
                        PhoneUtils.maskPhone(normalizedPhone));
                return;
            }

            // Create admin user
            User adminUser = new User();
            adminUser.setName(adminName);
            adminUser.setPhone(normalizedPhone);
            adminUser.setEmail(adminEmail);
            adminUser.setRole(Role.ADMIN);
            adminUser.setCreatedAt(Instant.now());
            adminUser.setUpdatedAt(Instant.now());

            User savedAdmin = userRepository.save(adminUser);

            log.info("✅ Created initial admin user: {} (Phone: {}, Email: {})",
                    adminName, PhoneUtils.maskPhone(normalizedPhone), adminEmail);
            log.info("Admin user ID: {}", savedAdmin.getId());

        } catch (Exception e) {
            log.error("Failed to seed admin user: {}", e.getMessage(), e);
        }
    }
}
