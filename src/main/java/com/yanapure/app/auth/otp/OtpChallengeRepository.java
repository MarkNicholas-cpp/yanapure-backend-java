package com.yanapure.app.auth.otp;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;

public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, UUID> {

    Optional<OtpChallenge> findTopByPhoneAndConsumedAtIsNullOrderByCreatedAtDesc(String phone);

    long countByPhoneAndConsumedAtIsNullAndExpiresAtAfter(String phone, Instant expiresAfter);

    @Modifying
    @Query("update OtpChallenge c set c.attemptCount = c.attemptCount + 1 where c.id = :id")
    int incrementAttemptCountById(@Param("id") UUID id);

    @Modifying
    @Query("update OtpChallenge c set c.consumedAt = CURRENT_TIMESTAMP, c.verified = true where c.id = :id")
    int markConsumedById(@Param("id") UUID id);
}
