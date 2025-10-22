package com.yanapure.app.auth.otp;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:h2:mem:testdb",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "spring.flyway.enabled=false"
    })
public class OtpChallengeRepositoryTest {

  @Autowired private TestEntityManager entityManager;

  @Autowired private OtpChallengeRepository otpChallengeRepository;

  @Test
  void testFindTopByPhoneAndConsumedAtIsNullOrderByCreatedAtDesc() {
    // Given
    String phone = "+14155552671";
    Instant now = Instant.now();

    OtpChallenge oldChallenge = new OtpChallenge();
    oldChallenge.setPhone(phone);
    oldChallenge.setCodeHash("hash1");
    oldChallenge.setExpiresAt(now.plusSeconds(300));
    oldChallenge.setCreatedAt(now.minusSeconds(100));
    entityManager.persistAndFlush(oldChallenge);

    OtpChallenge newChallenge = new OtpChallenge();
    newChallenge.setPhone(phone);
    newChallenge.setCodeHash("hash2");
    newChallenge.setExpiresAt(now.plusSeconds(300));
    newChallenge.setCreatedAt(now);
    entityManager.persistAndFlush(newChallenge);

    // When
    Optional<OtpChallenge> found =
        otpChallengeRepository.findTopByPhoneAndConsumedAtIsNullOrderByCreatedAtDesc(phone);

    // Then
    assertTrue(found.isPresent());
    assertEquals("hash2", found.get().getCodeHash());
  }

  @Test
  void testCountByPhoneAndConsumedAtIsNullAndExpiresAtAfter() {
    // Given
    String phone = "+14155552672";
    Instant now = Instant.now();

    OtpChallenge activeChallenge = new OtpChallenge();
    activeChallenge.setPhone(phone);
    activeChallenge.setCodeHash("hash1");
    activeChallenge.setExpiresAt(now.plusSeconds(300));
    entityManager.persistAndFlush(activeChallenge);

    OtpChallenge expiredChallenge = new OtpChallenge();
    expiredChallenge.setPhone(phone);
    expiredChallenge.setCodeHash("hash2");
    expiredChallenge.setExpiresAt(now.minusSeconds(100));
    entityManager.persistAndFlush(expiredChallenge);

    // When
    long count =
        otpChallengeRepository.countByPhoneAndConsumedAtIsNullAndExpiresAtAfter(phone, now);

    // Then
    assertEquals(1, count);
  }

  @Test
  void testFindByPhoneOrderByCreatedAtDesc() {
    // Given
    String phone = "+14155552673";
    Instant now = Instant.now();

    OtpChallenge challenge1 = new OtpChallenge();
    challenge1.setPhone(phone);
    challenge1.setCodeHash("hash1");
    challenge1.setExpiresAt(now.plusSeconds(300));
    challenge1.setCreatedAt(now.minusSeconds(100));
    entityManager.persistAndFlush(challenge1);

    OtpChallenge challenge2 = new OtpChallenge();
    challenge2.setPhone(phone);
    challenge2.setCodeHash("hash2");
    challenge2.setExpiresAt(now.plusSeconds(300));
    challenge2.setCreatedAt(now);
    entityManager.persistAndFlush(challenge2);

    // When
    List<OtpChallenge> challenges = otpChallengeRepository.findByPhoneOrderByCreatedAtDesc(phone);

    // Then
    assertEquals(2, challenges.size());
    assertEquals("hash2", challenges.get(0).getCodeHash()); // Most recent first
    assertEquals("hash1", challenges.get(1).getCodeHash());
  }

  @Test
  void testFindByExpiresAtBefore() {
    // Given
    Instant now = Instant.now();

    OtpChallenge expiredChallenge = new OtpChallenge();
    expiredChallenge.setPhone("+14155552674");
    expiredChallenge.setCodeHash("hash1");
    expiredChallenge.setExpiresAt(now.minusSeconds(100));
    entityManager.persistAndFlush(expiredChallenge);

    OtpChallenge activeChallenge = new OtpChallenge();
    activeChallenge.setPhone("+14155552675");
    activeChallenge.setCodeHash("hash2");
    activeChallenge.setExpiresAt(now.plusSeconds(300));
    entityManager.persistAndFlush(activeChallenge);

    // When
    List<OtpChallenge> expired = otpChallengeRepository.findByExpiresAtBefore(now);

    // Then
    assertEquals(1, expired.size());
    assertEquals("+14155552674", expired.get(0).getPhone());
  }

  @Test
  void testCountByPhoneAndCreatedAtAfter() {
    // Given
    String phone = "+14155552676";
    Instant now = Instant.now();
    Instant since = now.minusSeconds(200);

    OtpChallenge recentChallenge = new OtpChallenge();
    recentChallenge.setPhone(phone);
    recentChallenge.setCodeHash("hash1");
    recentChallenge.setExpiresAt(now.plusSeconds(300));
    recentChallenge.setCreatedAt(now.minusSeconds(100));
    entityManager.persistAndFlush(recentChallenge);

    OtpChallenge oldChallenge = new OtpChallenge();
    oldChallenge.setPhone(phone);
    oldChallenge.setCodeHash("hash2");
    oldChallenge.setExpiresAt(now.plusSeconds(300));
    oldChallenge.setCreatedAt(now.minusSeconds(300));
    entityManager.persistAndFlush(oldChallenge);

    // When
    long count = otpChallengeRepository.countByPhoneAndCreatedAtAfter(phone, since);

    // Then
    assertEquals(1, count);
  }
}
