package com.yanapure.app.auth.session;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
public class UserSessionRepositoryTest {

  @Autowired private TestEntityManager entityManager;

  @Autowired private UserSessionRepository userSessionRepository;

  @Test
  void testFindByAccessTokenAndActiveTrue() {
    // Given
    UUID userId = UUID.randomUUID();
    String accessToken = "access_token_123";
    String refreshToken = "refresh_token_123";
    Instant expiresAt = Instant.now().plusSeconds(3600);
    Instant refreshExpiresAt = Instant.now().plusSeconds(7200);

    UserSession session =
        new UserSession(
            userId,
            accessToken,
            refreshToken,
            expiresAt,
            refreshExpiresAt,
            "192.168.1.1",
            "Mozilla/5.0");
    entityManager.persistAndFlush(session);

    // When
    Optional<UserSession> found = userSessionRepository.findByAccessTokenAndActiveTrue(accessToken);

    // Then
    assertTrue(found.isPresent());
    assertEquals(userId, found.get().getUserId());
    assertEquals(accessToken, found.get().getAccessToken());
  }

  @Test
  void testFindByRefreshToken() {
    // Given
    UUID userId = UUID.randomUUID();
    String accessToken = "access_token_456";
    String refreshToken = "refresh_token_456";
    Instant expiresAt = Instant.now().plusSeconds(3600);
    Instant refreshExpiresAt = Instant.now().plusSeconds(7200);

    UserSession session =
        new UserSession(
            userId,
            accessToken,
            refreshToken,
            expiresAt,
            refreshExpiresAt,
            "192.168.1.2",
            "Mozilla/5.0");
    entityManager.persistAndFlush(session);

    // When
    Optional<UserSession> found = userSessionRepository.findByRefreshToken(refreshToken);

    // Then
    assertTrue(found.isPresent());
    assertEquals(userId, found.get().getUserId());
    assertEquals(refreshToken, found.get().getRefreshToken());
  }

  @Test
  void testFindByUserIdAndActiveTrueOrderByCreatedAtDesc() {
    // Given
    UUID userId = UUID.randomUUID();
    Instant now = Instant.now();

    UserSession session1 =
        new UserSession(
            userId,
            "token1",
            "refresh1",
            now.plusSeconds(3600),
            now.plusSeconds(7200),
            "192.168.1.1",
            "Mozilla/5.0");
    session1.setCreatedAt(now.minusSeconds(100));
    entityManager.persistAndFlush(session1);

    UserSession session2 =
        new UserSession(
            userId,
            "token2",
            "refresh2",
            now.plusSeconds(3600),
            now.plusSeconds(7200),
            "192.168.1.2",
            "Mozilla/5.0");
    session2.setCreatedAt(now);
    entityManager.persistAndFlush(session2);

    // When
    List<UserSession> sessions =
        userSessionRepository.findByUserIdAndActiveTrueOrderByCreatedAtDesc(userId);

    // Then
    assertEquals(2, sessions.size());
    assertEquals("token2", sessions.get(0).getAccessToken()); // Most recent first
    assertEquals("token1", sessions.get(1).getAccessToken());
  }

  @Test
  void testFindByExpiresAtBefore() {
    // Given
    Instant now = Instant.now();

    UserSession expiredSession =
        new UserSession(
            UUID.randomUUID(),
            "token1",
            "refresh1",
            now.minusSeconds(100),
            now.plusSeconds(7200),
            "192.168.1.1",
            "Mozilla/5.0");
    entityManager.persistAndFlush(expiredSession);

    UserSession activeSession =
        new UserSession(
            UUID.randomUUID(),
            "token2",
            "refresh2",
            now.plusSeconds(3600),
            now.plusSeconds(7200),
            "192.168.1.2",
            "Mozilla/5.0");
    entityManager.persistAndFlush(activeSession);

    // When
    List<UserSession> expired = userSessionRepository.findByExpiresAtBefore(now);

    // Then
    assertEquals(1, expired.size());
    assertEquals("token1", expired.get(0).getAccessToken());
  }

  @Test
  void testCountByUserIdAndActiveTrue() {
    // Given
    UUID userId = UUID.randomUUID();
    Instant now = Instant.now();

    UserSession activeSession1 =
        new UserSession(
            userId,
            "token1",
            "refresh1",
            now.plusSeconds(3600),
            now.plusSeconds(7200),
            "192.168.1.1",
            "Mozilla/5.0");
    entityManager.persistAndFlush(activeSession1);

    UserSession activeSession2 =
        new UserSession(
            userId,
            "token2",
            "refresh2",
            now.plusSeconds(3600),
            now.plusSeconds(7200),
            "192.168.1.2",
            "Mozilla/5.0");
    entityManager.persistAndFlush(activeSession2);

    UserSession inactiveSession =
        new UserSession(
            userId,
            "token3",
            "refresh3",
            now.plusSeconds(3600),
            now.plusSeconds(7200),
            "192.168.1.3",
            "Mozilla/5.0");
    inactiveSession.setActive(false);
    entityManager.persistAndFlush(inactiveSession);

    // When
    long count = userSessionRepository.countByUserIdAndActiveTrue(userId);

    // Then
    assertEquals(2, count);
  }

  @Test
  void testFindByClientIpAndCreatedAtAfter() {
    // Given
    String clientIp = "192.168.1.100";
    Instant now = Instant.now();
    Instant since = now.minusSeconds(200);

    UserSession recentSession =
        new UserSession(
            UUID.randomUUID(),
            "token1",
            "refresh1",
            now.plusSeconds(3600),
            now.plusSeconds(7200),
            clientIp,
            "Mozilla/5.0");
    recentSession.setCreatedAt(now.minusSeconds(100));
    entityManager.persistAndFlush(recentSession);

    UserSession oldSession =
        new UserSession(
            UUID.randomUUID(),
            "token2",
            "refresh2",
            now.plusSeconds(3600),
            now.plusSeconds(7200),
            clientIp,
            "Mozilla/5.0");
    oldSession.setCreatedAt(now.minusSeconds(300));
    entityManager.persistAndFlush(oldSession);

    // When
    List<UserSession> sessions =
        userSessionRepository.findByClientIpAndCreatedAtAfter(clientIp, since);

    // Then
    assertEquals(1, sessions.size());
    assertEquals("token1", sessions.get(0).getAccessToken());
  }
}
