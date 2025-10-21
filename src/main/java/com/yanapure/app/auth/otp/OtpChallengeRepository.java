package com.yanapure.app.auth.otp;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;

public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, UUID> {

    /**
     * Find the latest active OTP challenge for a phone number
     */
    Optional<OtpChallenge> findTopByPhoneAndConsumedAtIsNullOrderByCreatedAtDesc(String phone);

    /**
     * Count active OTP challenges for a phone number
     */
    long countByPhoneAndConsumedAtIsNullAndExpiresAtAfter(String phone, Instant expiresAfter);

    /**
     * Increment attempt count for an OTP challenge
     */
    @Modifying
    @Query("update OtpChallenge c set c.attemptCount = c.attemptCount + 1 where c.id = :id")
    int incrementAttemptCountById(@Param("id") UUID id);

    /**
     * Mark an OTP challenge as consumed
     */
    @Modifying
    @Query("update OtpChallenge c set c.consumedAt = :consumedAt, c.verified = true where c.id = :id")
    int markConsumedById(@Param("id") UUID id, @Param("consumedAt") Instant consumedAt);

    /**
     * Find all OTP challenges for a phone number (for audit/cleanup)
     */
    List<OtpChallenge> findByPhoneOrderByCreatedAtDesc(String phone);

    /**
     * Find expired OTP challenges (for cleanup)
     */
    List<OtpChallenge> findByExpiresAtBefore(Instant expiresBefore);

    /**
     * Count total OTP challenges sent to a phone number in a time window
     */
    @Query("SELECT COUNT(c) FROM OtpChallenge c WHERE c.phone = :phone AND c.createdAt >= :since")
    long countByPhoneAndCreatedAtAfter(@Param("phone") String phone, @Param("since") Instant since);

    /**
     * Find OTP challenges with high attempt count (potential abuse)
     */
    @Query("SELECT c FROM OtpChallenge c WHERE c.attemptCount >= :minAttempts AND c.consumedAt IS NULL")
    List<OtpChallenge> findHighAttemptChallenges(@Param("minAttempts") int minAttempts);

    /**
     * Delete expired OTP challenges (cleanup)
     */
    @Modifying
    @Query("DELETE FROM OtpChallenge c WHERE c.expiresAt < :expiresBefore")
    int deleteExpiredChallenges(@Param("expiresBefore") Instant expiresBefore);

    /**
     * Find OTP challenges by request IP (for rate limiting)
     */
    List<OtpChallenge> findByRequestIpAndCreatedAtAfter(String requestIp, Instant since);
}
