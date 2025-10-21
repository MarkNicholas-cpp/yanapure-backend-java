package com.yanapure.app.auth.service;

import com.yanapure.app.auth.session.UserSession;
import com.yanapure.app.auth.session.UserSessionRepository;
import com.yanapure.app.common.ApiException;
import com.yanapure.app.users.Role;
import com.yanapure.app.users.User;
import com.yanapure.app.users.UserRepository;
import com.yanapure.app.util.PhoneUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Main authentication service that orchestrates the auth flow
 */
@Service
@Transactional
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final OtpService otpService;
    private final JwtService jwtService;

    @Value("${app.auth.access-token-expiry-hours:1}")
    private int accessTokenExpiryHours;

    @Value("${app.auth.refresh-token-expiry-days:7}")
    private int refreshTokenExpiryDays;

    @Value("${app.auth.max-sessions-per-user:5}")
    private int maxSessionsPerUser;

    public AuthenticationService(UserRepository userRepository,
            UserSessionRepository userSessionRepository,
            OtpService otpService,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
        this.otpService = otpService;
        this.jwtService = jwtService;
    }

    /**
     * Initiate phone-based authentication by sending OTP
     */
    public void initiatePhoneAuth(String phoneNumber, String clientIp) {
        String normalizedPhone = PhoneUtils.normalizeToE164(phoneNumber);

        log.info("Initiating phone auth for: {}", PhoneUtils.maskPhone(normalizedPhone));

        // Send OTP
        otpService.sendOtp(normalizedPhone, clientIp);
    }

    /**
     * Complete phone-based authentication with OTP verification
     */
    public AuthResult verifyPhoneAndLogin(String phoneNumber, String otpCode,
            String clientIp, String userAgent) {
        String normalizedPhone = PhoneUtils.normalizeToE164(phoneNumber);

        log.info("Verifying phone auth for: {}", PhoneUtils.maskPhone(normalizedPhone));

        // Verify OTP
        boolean otpValid = otpService.verifyOtp(normalizedPhone, otpCode, clientIp);
        if (!otpValid) {
            throw new ApiException("INVALID_OTP", "Invalid verification code");
        }

        // Find or create user
        User user = findOrCreateUser(normalizedPhone);

        // Update last login
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // Create session with tokens
        UserSession session = createUserSession(user, accessToken, refreshToken, clientIp, userAgent);

        log.info("User authenticated successfully: {} (ID: {})",
                PhoneUtils.maskPhone(normalizedPhone), user.getId());

        return new AuthResult(user, accessToken, refreshToken, session.getId());
    }

    /**
     * Refresh access token using refresh token
     */
    public AuthResult refreshToken(String refreshToken, String clientIp) {
        log.info("Refreshing token for client IP: {}", clientIp);

        // Find session by refresh token
        Optional<UserSession> sessionOpt = userSessionRepository.findByRefreshToken(refreshToken);
        if (sessionOpt.isEmpty()) {
            throw new ApiException("INVALID_REFRESH_TOKEN", "Invalid refresh token");
        }

        UserSession session = sessionOpt.get();

        // Check if session is still valid
        if (!session.getActive() || session.isRefreshExpired()) {
            throw new ApiException("REFRESH_TOKEN_EXPIRED", "Refresh token has expired");
        }

        // Get user
        Optional<User> userOpt = userRepository.findById(session.getUserId());
        if (userOpt.isEmpty()) {
            throw new ApiException("USER_NOT_FOUND", "User not found");
        }

        User user = userOpt.get();

        // Generate new tokens
        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        // Update session
        session.setAccessToken(newAccessToken);
        session.setRefreshToken(newRefreshToken);
        session.setExpiresAt(Instant.now().plusSeconds(accessTokenExpiryHours * 3600));
        session.setRefreshExpiresAt(Instant.now().plusSeconds(refreshTokenExpiryDays * 24 * 3600));
        session.updateLastUsed();
        userSessionRepository.save(session);

        log.info("Token refreshed successfully for user: {}", user.getId());

        return new AuthResult(user, newAccessToken, newRefreshToken, session.getId());
    }

    /**
     * Logout user (deactivate session)
     */
    public void logout(String accessToken) {
        log.info("Logging out user");

        Optional<UserSession> sessionOpt = userSessionRepository.findByAccessTokenAndActiveTrue(accessToken);
        if (sessionOpt.isPresent()) {
            UserSession session = sessionOpt.get();
            session.deactivate();
            userSessionRepository.save(session);
            log.info("User logged out successfully: {}", session.getUserId());
        } else {
            throw new ApiException("INVALID_TOKEN", "Invalid or expired token");
        }
    }

    /**
     * Logout from all devices
     */
    public void logoutAllDevices(UUID userId) {
        log.info("Logging out user from all devices: {}", userId);

        int deactivated = userSessionRepository.deactivateAllSessionsForUser(userId);
        log.info("Deactivated {} sessions for user: {}", deactivated, userId);
    }

    /**
     * Validate access token and return user
     */
    public User validateToken(String accessToken) {
        Optional<UserSession> sessionOpt = userSessionRepository.findByAccessTokenAndActiveTrue(accessToken);
        if (sessionOpt.isEmpty()) {
            throw new ApiException("INVALID_TOKEN", "Invalid or expired token");
        }

        UserSession session = sessionOpt.get();
        if (session.isExpired()) {
            throw new ApiException("TOKEN_EXPIRED", "Token has expired");
        }

        Optional<User> userOpt = userRepository.findById(session.getUserId());
        if (userOpt.isEmpty()) {
            throw new ApiException("USER_NOT_FOUND", "User not found");
        }

        // Update last used
        session.updateLastUsed();
        userSessionRepository.save(session);

        return userOpt.get();
    }

    /**
     * Get user sessions
     */
    public List<UserSession> getUserSessions(UUID userId) {
        return userSessionRepository.findByUserIdAndActiveTrueOrderByCreatedAtDesc(userId);
    }

    /**
     * Clean up expired sessions
     */
    public int cleanupExpiredSessions() {
        Instant cutoff = Instant.now();

        int deleted = userSessionRepository.deleteExpiredSessions(cutoff);
        log.info("Cleaned up {} expired sessions", deleted);

        return deleted;
    }

    /**
     * Find or create user by phone number
     */
    private User findOrCreateUser(String phoneNumber) {
        Optional<User> userOpt = userRepository.findByPhone(phoneNumber);

        if (userOpt.isPresent()) {
            return userOpt.get();
        }

        // Create new user
        User newUser = new User();
        newUser.setName("User"); // Default name, can be updated later
        newUser.setPhone(phoneNumber);
        newUser.setRole(Role.USER);
        newUser.setCreatedAt(Instant.now());
        newUser.setUpdatedAt(Instant.now());

        User savedUser = userRepository.save(newUser);
        log.info("Created new user: {} (ID: {})", PhoneUtils.maskPhone(phoneNumber), savedUser.getId());

        return savedUser;
    }

    /**
     * Create user session
     */
    private UserSession createUserSession(User user, String accessToken, String refreshToken, String clientIp,
            String userAgent) {
        // Check session limit
        long activeSessions = userSessionRepository.countByUserIdAndActiveTrue(user.getId());
        if (activeSessions >= maxSessionsPerUser) {
            // Deactivate oldest session
            List<UserSession> sessions = userSessionRepository
                    .findByUserIdAndActiveTrueOrderByCreatedAtDesc(user.getId());
            if (!sessions.isEmpty()) {
                UserSession oldestSession = sessions.get(sessions.size() - 1);
                oldestSession.deactivate();
                userSessionRepository.save(oldestSession);
                log.info("Deactivated oldest session for user: {}", user.getId());
            }
        }

        // Create new session
        Instant expiresAt = Instant.now().plusSeconds(accessTokenExpiryHours * 3600);
        Instant refreshExpiresAt = Instant.now().plusSeconds(refreshTokenExpiryDays * 24 * 3600);

        UserSession session = new UserSession(user.getId(), accessToken, refreshToken, expiresAt, refreshExpiresAt,
                clientIp, userAgent);
        return userSessionRepository.save(session);
    }

    /**
     * Result of authentication operation
     */
    public static class AuthResult {
        private final User user;
        private final String accessToken;
        private final String refreshToken;
        private final UUID sessionId;

        public AuthResult(User user, String accessToken, String refreshToken, UUID sessionId) {
            this.user = user;
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.sessionId = sessionId;
        }

        public User getUser() {
            return user;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public UUID getSessionId() {
            return sessionId;
        }
    }
}
