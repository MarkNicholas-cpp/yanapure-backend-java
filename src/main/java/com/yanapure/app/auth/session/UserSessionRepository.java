package com.yanapure.app.auth.session;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;

/**
 * Repository for managing user sessions
 */
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    /**
     * Find active session by access token
     */
    Optional<UserSession> findByAccessTokenAndActiveTrue(String accessToken);

    /**
     * Find session by refresh token
     */
    Optional<UserSession> findByRefreshToken(String refreshToken);

    /**
     * Find all active sessions for a user
     */
    List<UserSession> findByUserIdAndActiveTrueOrderByCreatedAtDesc(UUID userId);

    /**
     * Find expired sessions (for cleanup)
     */
    List<UserSession> findByExpiresAtBefore(Instant expiresBefore);

    /**
     * Find sessions by user ID and client IP
     */
    List<UserSession> findByUserIdAndClientIpAndActiveTrue(UUID userId, String clientIp);

    /**
     * Count active sessions for a user
     */
    long countByUserIdAndActiveTrue(UUID userId);

    /**
     * Deactivate all sessions for a user (logout from all devices)
     */
    @Modifying
    @Query("UPDATE UserSession s SET s.active = false WHERE s.userId = :userId")
    int deactivateAllSessionsForUser(@Param("userId") UUID userId);

    /**
     * Deactivate a specific session
     */
    @Modifying
    @Query("UPDATE UserSession s SET s.active = false WHERE s.id = :sessionId")
    int deactivateSession(@Param("sessionId") UUID sessionId);

    /**
     * Update last used timestamp
     */
    @Modifying
    @Query("UPDATE UserSession s SET s.lastUsedAt = :lastUsed WHERE s.id = :sessionId")
    int updateLastUsed(@Param("sessionId") UUID sessionId, @Param("lastUsed") Instant lastUsed);

    /**
     * Delete expired sessions (cleanup)
     */
    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.expiresAt < :expiresBefore")
    int deleteExpiredSessions(@Param("expiresBefore") Instant expiresBefore);

    /**
     * Find sessions by client IP (for rate limiting)
     */
    List<UserSession> findByClientIpAndCreatedAtAfter(String clientIp, Instant since);

    /**
     * Find sessions that haven't been used recently (for cleanup)
     */
    @Query("SELECT s FROM UserSession s WHERE s.lastUsedAt < :lastUsedBefore AND s.active = true")
    List<UserSession> findInactiveSessions(@Param("lastUsedBefore") Instant lastUsedBefore);
}
