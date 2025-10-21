package com.yanapure.app.users;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByPhone(String phone);

    boolean existsByPhone(String phone);

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * Update user's last login timestamp
     */
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :loginTime, u.updatedAt = :loginTime WHERE u.id = :userId")
    int updateLastLogin(@Param("userId") UUID userId, @Param("loginTime") Instant loginTime);

    /**
     * Find users by role
     */
    List<User> findByRole(Role role);

    /**
     * Count users by role
     */
    long countByRole(Role role);

    /**
     * Find users created after a specific date
     */
    List<User> findByCreatedAtAfter(Instant createdAt);

    /**
     * Count users created after a specific date
     */
    long countByCreatedAtAfter(Instant createdAt);

    /**
     * Find users with email (not null and not blank)
     */
    @Query("SELECT u FROM User u WHERE u.email IS NOT NULL AND u.email != ''")
    List<User> findUsersWithEmail();
}
