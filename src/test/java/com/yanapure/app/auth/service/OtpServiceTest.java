package com.yanapure.app.auth.service;

import com.yanapure.app.auth.otp.OtpChallenge;
import com.yanapure.app.auth.otp.OtpChallengeRepository;
import com.yanapure.app.common.ApiException;
import com.yanapure.app.sms.InMemorySmsProvider;
import com.yanapure.app.sms.SmsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OtpServiceTest {

    @Mock
    private OtpChallengeRepository otpChallengeRepository;

    private InMemorySmsProvider smsProvider;
    private OtpService otpService;

    @BeforeEach
    void setUp() {
        smsProvider = new InMemorySmsProvider();
        otpService = new OtpService(otpChallengeRepository, smsProvider);

        // Set test configuration
        ReflectionTestUtils.setField(otpService, "otpLength", 6);
        ReflectionTestUtils.setField(otpService, "otpExpiryMinutes", 5);
        ReflectionTestUtils.setField(otpService, "maxAttempts", 3);
        ReflectionTestUtils.setField(otpService, "rateLimitMinutes", 1);
        ReflectionTestUtils.setField(otpService, "maxPerHour", 5);
    }

    @Test
    void testSendOtpSuccess() {
        // Given
        String phoneNumber = "+14155552671";
        String clientIp = "192.168.1.1";

        when(otpChallengeRepository.countByPhoneAndCreatedAtAfter(anyString(), any(Instant.class)))
                .thenReturn(0L);
        when(otpChallengeRepository.findByRequestIpAndCreatedAtAfter(anyString(), any(Instant.class)))
                .thenReturn(java.util.Collections.emptyList());
        when(otpChallengeRepository.save(any(OtpChallenge.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        assertDoesNotThrow(() -> otpService.sendOtp(phoneNumber, clientIp));

        // Then
        verify(otpChallengeRepository).save(any(OtpChallenge.class));
        assertEquals(1, smsProvider.getMessageCount(phoneNumber));
        assertTrue(smsProvider.getLastMessage(phoneNumber).contains("Your Yana verification code is:"));
    }

    @Test
    void testSendOtpWithInvalidPhone() {
        // Given
        String invalidPhone = "invalid-phone";
        String clientIp = "192.168.1.1";

        // When & Then
        assertThrows(ApiException.class, () -> otpService.sendOtp(invalidPhone, clientIp));
    }

    @Test
    void testVerifyOtpSuccess() {
        // Given
        String phoneNumber = "+14155552671";
        String otpCode = "123456";
        String clientIp = "192.168.1.1";

        OtpChallenge challenge = new OtpChallenge();
        challenge.setPhone(phoneNumber);
        challenge.setCodeHash(String.valueOf(otpCode.hashCode()));
        challenge.setExpiresAt(Instant.now().plusSeconds(300));
        challenge.setAttemptCount(0);
        challenge.setVerified(false);

        when(otpChallengeRepository.findTopByPhoneAndConsumedAtIsNullOrderByCreatedAtDesc(phoneNumber))
                .thenReturn(Optional.of(challenge));
        when(otpChallengeRepository.save(any(OtpChallenge.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        boolean result = otpService.verifyOtp(phoneNumber, otpCode, clientIp);

        // Then
        assertTrue(result);
        assertTrue(challenge.isVerified());
        assertNotNull(challenge.getConsumedAt());
    }

    @Test
    void testVerifyOtpWithInvalidCode() {
        // Given
        String phoneNumber = "+14155552671";
        String correctCode = "123456";
        String wrongCode = "654321";
        String clientIp = "192.168.1.1";

        OtpChallenge challenge = new OtpChallenge();
        challenge.setPhone(phoneNumber);
        challenge.setCodeHash(String.valueOf(correctCode.hashCode()));
        challenge.setExpiresAt(Instant.now().plusSeconds(300));
        challenge.setAttemptCount(0);
        challenge.setVerified(false);

        when(otpChallengeRepository.findTopByPhoneAndConsumedAtIsNullOrderByCreatedAtDesc(phoneNumber))
                .thenReturn(Optional.of(challenge));
        when(otpChallengeRepository.save(any(OtpChallenge.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        boolean result = otpService.verifyOtp(phoneNumber, wrongCode, clientIp);

        // Then
        assertFalse(result);
        assertEquals(1, challenge.getAttemptCount());
        assertFalse(challenge.isVerified());
    }

    @Test
    void testVerifyOtpWithExpiredChallenge() {
        // Given
        String phoneNumber = "+14155552671";
        String otpCode = "123456";
        String clientIp = "192.168.1.1";

        OtpChallenge challenge = new OtpChallenge();
        challenge.setPhone(phoneNumber);
        challenge.setCodeHash(String.valueOf(otpCode.hashCode()));
        challenge.setExpiresAt(Instant.now().minusSeconds(100)); // Expired
        challenge.setAttemptCount(0);
        challenge.setVerified(false);

        when(otpChallengeRepository.findTopByPhoneAndConsumedAtIsNullOrderByCreatedAtDesc(phoneNumber))
                .thenReturn(Optional.of(challenge));

        // When & Then
        assertThrows(ApiException.class, () -> otpService.verifyOtp(phoneNumber, otpCode, clientIp));
    }

    @Test
    void testVerifyOtpWithMaxAttemptsExceeded() {
        // Given
        String phoneNumber = "+14155552671";
        String otpCode = "123456";
        String clientIp = "192.168.1.1";

        OtpChallenge challenge = new OtpChallenge();
        challenge.setPhone(phoneNumber);
        challenge.setCodeHash(String.valueOf(otpCode.hashCode()));
        challenge.setExpiresAt(Instant.now().plusSeconds(300));
        challenge.setAttemptCount(3); // Max attempts reached
        challenge.setVerified(false);

        when(otpChallengeRepository.findTopByPhoneAndConsumedAtIsNullOrderByCreatedAtDesc(phoneNumber))
                .thenReturn(Optional.of(challenge));

        // When & Then
        assertThrows(ApiException.class, () -> otpService.verifyOtp(phoneNumber, otpCode, clientIp));
    }

    @Test
    void testVerifyOtpWithNoActiveChallenge() {
        // Given
        String phoneNumber = "+14155552671";
        String otpCode = "123456";
        String clientIp = "192.168.1.1";

        when(otpChallengeRepository.findTopByPhoneAndConsumedAtIsNullOrderByCreatedAtDesc(phoneNumber))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(ApiException.class, () -> otpService.verifyOtp(phoneNumber, otpCode, clientIp));
    }

    @Test
    void testHasValidOtp() {
        // Given
        String phoneNumber = "+14155552671";

        OtpChallenge challenge = new OtpChallenge();
        challenge.setPhone(phoneNumber);
        challenge.setExpiresAt(Instant.now().plusSeconds(300));
        challenge.setAttemptCount(1);
        challenge.setVerified(false);

        when(otpChallengeRepository.findTopByPhoneAndConsumedAtIsNullOrderByCreatedAtDesc(phoneNumber))
                .thenReturn(Optional.of(challenge));

        // When
        boolean hasValid = otpService.hasValidOtp(phoneNumber);

        // Then
        assertTrue(hasValid);
    }

    @Test
    void testHasValidOtpWithExpiredChallenge() {
        // Given
        String phoneNumber = "+14155552671";

        OtpChallenge challenge = new OtpChallenge();
        challenge.setPhone(phoneNumber);
        challenge.setExpiresAt(Instant.now().minusSeconds(100)); // Expired
        challenge.setAttemptCount(1);
        challenge.setVerified(false);

        when(otpChallengeRepository.findTopByPhoneAndConsumedAtIsNullOrderByCreatedAtDesc(phoneNumber))
                .thenReturn(Optional.of(challenge));

        // When
        boolean hasValid = otpService.hasValidOtp(phoneNumber);

        // Then
        assertFalse(hasValid);
    }
}
