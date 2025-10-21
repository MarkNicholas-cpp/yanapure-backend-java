package com.yanapure.app.auth.service;

import com.yanapure.app.auth.otp.OtpChallenge;
import com.yanapure.app.auth.otp.OtpChallengeRepository;
import com.yanapure.app.common.ApiException;
import com.yanapure.app.sms.SmsProvider;
import com.yanapure.app.util.PhoneUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing OTP challenges and phone-based authentication
 */
@Service
@Transactional
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpChallengeRepository otpChallengeRepository;
    private final SmsProvider smsProvider;

    @Value("${app.otp.length:6}")
    private int otpLength;

    @Value("${app.otp.expiry-minutes:5}")
    private int otpExpiryMinutes;

    @Value("${app.otp.max-attempts:3}")
    private int maxAttempts;

    @Value("${app.otp.rate-limit-minutes:1}")
    private int rateLimitMinutes;

    @Value("${app.otp.max-per-hour:5}")
    private int maxPerHour;

    public OtpService(OtpChallengeRepository otpChallengeRepository, SmsProvider smsProvider) {
        this.otpChallengeRepository = otpChallengeRepository;
        this.smsProvider = smsProvider;
    }

    /**
     * Generate and send OTP to phone number
     */
    public void sendOtp(String phoneNumber, String clientIp) {
        String normalizedPhone = PhoneUtils.normalizeToE164(phoneNumber);

        log.info("Sending OTP to phone: {}", PhoneUtils.maskPhone(normalizedPhone));

        // Check rate limiting
        checkRateLimit(normalizedPhone, clientIp);

        // Generate OTP code
        String otpCode = generateOtpCode();
        String codeHash = hashOtpCode(otpCode);

        // Create OTP challenge
        OtpChallenge challenge = new OtpChallenge();
        challenge.setPhone(normalizedPhone);
        challenge.setCodeHash(codeHash);
        challenge.setExpiresAt(Instant.now().plusSeconds(otpExpiryMinutes * 60));
        challenge.setRequestIp(clientIp);
        challenge.setAttemptCount(0);
        challenge.setVerified(false);

        // Save challenge
        otpChallengeRepository.save(challenge);

        // Send SMS
        String message = String.format("Your Yana verification code is: %s. Valid for %d minutes.",
                otpCode, otpExpiryMinutes);

        // TEMPORARY: Log OTP to console for testing (REMOVE IN PRODUCTION)
        log.info("🔑 OTP CODE FOR TESTING: {} (Phone: {})", otpCode, PhoneUtils.maskPhone(normalizedPhone));
        System.out.println(
                "🔑 OTP CODE FOR TESTING: " + otpCode + " (Phone: " + PhoneUtils.maskPhone(normalizedPhone) + ")");

        boolean sent = smsProvider.sendSms(normalizedPhone, message);
        if (!sent) {
            log.error("Failed to send OTP SMS to: {}", PhoneUtils.maskPhone(normalizedPhone));
            throw new ApiException("SMS_SEND_FAILED", "Failed to send verification code");
        }

        log.info("OTP sent successfully to: {}", PhoneUtils.maskPhone(normalizedPhone));
    }

    /**
     * Verify OTP code
     */
    public boolean verifyOtp(String phoneNumber, String otpCode, String clientIp) {
        String normalizedPhone = PhoneUtils.normalizeToE164(phoneNumber);

        log.info("Verifying OTP for phone: {}", PhoneUtils.maskPhone(normalizedPhone));

        // Find latest active challenge
        Optional<OtpChallenge> challengeOpt = otpChallengeRepository
                .findTopByPhoneAndConsumedAtIsNullOrderByCreatedAtDesc(normalizedPhone);

        if (challengeOpt.isEmpty()) {
            log.warn("No active OTP challenge found for phone: {}", PhoneUtils.maskPhone(normalizedPhone));
            throw new ApiException("OTP_NOT_FOUND", "No verification code found. Please request a new one.");
        }

        OtpChallenge challenge = challengeOpt.get();

        // Check if expired
        if (challenge.isExpired()) {
            log.warn("OTP challenge expired for phone: {}", PhoneUtils.maskPhone(normalizedPhone));
            throw new ApiException("OTP_EXPIRED", "Verification code has expired. Please request a new one.");
        }

        // Check attempt limit
        if (challenge.getAttemptCount() >= maxAttempts) {
            log.warn("OTP attempt limit exceeded for phone: {}", PhoneUtils.maskPhone(normalizedPhone));
            throw new ApiException("OTP_ATTEMPTS_EXCEEDED", "Too many attempts. Please request a new code.");
        }

        // Increment attempt count
        challenge.incrementAttemptCount();
        otpChallengeRepository.save(challenge);

        // Verify code
        String providedHash = hashOtpCode(otpCode);
        boolean isValid = providedHash.equals(challenge.getCodeHash());

        if (isValid) {
            // Mark as consumed
            challenge.markConsumed();
            otpChallengeRepository.save(challenge);
            log.info("OTP verified successfully for phone: {}", PhoneUtils.maskPhone(normalizedPhone));
        } else {
            log.warn("Invalid OTP provided for phone: {}", PhoneUtils.maskPhone(normalizedPhone));
        }

        return isValid;
    }

    /**
     * Check if phone has a valid OTP challenge
     */
    public boolean hasValidOtp(String phoneNumber) {
        String normalizedPhone = PhoneUtils.normalizeToE164(phoneNumber);

        Optional<OtpChallenge> challengeOpt = otpChallengeRepository
                .findTopByPhoneAndConsumedAtIsNullOrderByCreatedAtDesc(normalizedPhone);

        return challengeOpt.isPresent() &&
                !challengeOpt.get().isExpired() &&
                challengeOpt.get().getAttemptCount() < maxAttempts;
    }

    /**
     * Clean up expired OTP challenges
     */
    public int cleanupExpiredChallenges() {
        Instant cutoff = Instant.now();

        int deleted = otpChallengeRepository.deleteExpiredChallenges(cutoff);
        log.info("Cleaned up {} expired OTP challenges", deleted);

        return deleted;
    }

    /**
     * Check rate limiting for phone number
     */
    private void checkRateLimit(String phoneNumber, String clientIp) {
        Instant oneHourAgo = Instant.now().minusSeconds(3600);
        Instant oneMinuteAgo = Instant.now().minusSeconds(60);

        // Check per-phone rate limit
        long recentCount = otpChallengeRepository.countByPhoneAndCreatedAtAfter(phoneNumber, oneHourAgo);
        if (recentCount >= maxPerHour) {
            log.warn("Rate limit exceeded for phone: {} ({} requests in last hour)",
                    PhoneUtils.maskPhone(phoneNumber), recentCount);
            throw new ApiException("RATE_LIMIT_EXCEEDED",
                    "Too many verification requests. Please try again later.");
        }

        // Check recent requests (1 minute cooldown)
        long recentMinuteCount = otpChallengeRepository.countByPhoneAndCreatedAtAfter(phoneNumber, oneMinuteAgo);
        if (recentMinuteCount > 0) {
            log.warn("Recent request found for phone: {}", PhoneUtils.maskPhone(phoneNumber));
            throw new ApiException("RATE_LIMIT_EXCEEDED",
                    "Please wait before requesting another verification code.");
        }

        // Check IP-based rate limiting
        List<OtpChallenge> ipRecent = otpChallengeRepository.findByRequestIpAndCreatedAtAfter(clientIp, oneMinuteAgo);
        if (ipRecent.size() >= 3) { // Max 3 requests per IP per minute
            log.warn("IP rate limit exceeded for IP: {}", clientIp);
            throw new ApiException("RATE_LIMIT_EXCEEDED",
                    "Too many requests from this IP. Please try again later.");
        }
    }

    /**
     * Generate random OTP code
     */
    private String generateOtpCode() {
        int min = (int) Math.pow(10, otpLength - 1);
        int max = (int) Math.pow(10, otpLength) - 1;
        int code = RANDOM.nextInt(max - min + 1) + min;
        return String.valueOf(code);
    }

    /**
     * Hash OTP code for secure storage
     */
    private String hashOtpCode(String otpCode) {
        // Simple hash for demo - in production, use proper hashing like BCrypt
        return String.valueOf(otpCode.hashCode());
    }
}
