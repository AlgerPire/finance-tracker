package com.finance_tracker.backend_server.security.service;

import com.finance_tracker.backend_server.auth.entity.RefreshToken;
import com.finance_tracker.backend_server.auth.exception.TokenRefreshException;
import com.finance_tracker.backend_server.auth.repository.RefreshTokenRepository;
import com.finance_tracker.backend_server.user.entity.User;
import com.finance_tracker.backend_server.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing refresh tokens.
 * <p>
 * This service handles the creation, validation, and deletion of refresh tokens
 * used in JWT-based authentication. Each user can have only one active refresh
 * token at a time.
 * </p>
 *
 * @author Finance Tracker Team
 * @version 1.0
 * @see com.finance_tracker.backend_server.auth.entity.RefreshToken
 * @see com.finance_tracker.backend_server.auth.exception.TokenRefreshException
 * @since 2024
 */
@Service
@Transactional
public class RefreshTokenService {

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenService.class);

    @Value("${finance_tracker.app.jwtRefreshExpirationMs}")
    private Long refreshTokenDurationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    /**
     * Constructs a RefreshTokenService with required dependencies.
     *
     * @param refreshTokenRepository repository for refresh token data access
     * @param userRepository         repository for user data access
     */
    @Autowired
    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, UserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    /**
     * Finds a refresh token by its token string.
     *
     * @param token the token string to search for
     * @return an Optional containing the refresh token if found
     */
    @Transactional(readOnly = true)
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    /**
     * Creates a new refresh token for the specified user.
     * <p>
     * If the user already has an existing refresh token, it will be deleted
     * and replaced with a new one.
     * </p>
     *
     * @param userId the ID of the user to create a token for
     * @return the newly created refresh token
     * @throws IllegalArgumentException if user is not found
     */
    public RefreshToken createRefreshToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));


        Optional<RefreshToken> existingToken = refreshTokenRepository.findByUser(user);
        existingToken.ifPresent(token -> {
            refreshTokenRepository.delete(token);
            refreshTokenRepository.flush();
            logger.debug("Deleted existing refresh token for user: {}", user.getUsername());
        });

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))
                .token(UUID.randomUUID().toString())
                .build();

        RefreshToken savedToken = refreshTokenRepository.save(refreshToken);
        logger.debug("Created new refresh token for user: {}", user.getUsername());

        return savedToken;
    }

    /**
     * Verifies that the refresh token has not expired.
     * <p>
     * If the token is expired, it will be deleted from the database
     * and a {@link com.finance_tracker.backend_server.auth.exception.TokenRefreshException} will be thrown.
     * </p>
     *
     * @param token the refresh token to verify
     * @return the same token if valid
     * @throws com.finance_tracker.backend_server.auth.exception.TokenRefreshException if the token has expired
     */
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            logger.warn("Refresh token expired for user: {}", token.getUser().getUsername());
            throw new TokenRefreshException(
                    token.getToken(),
                    "Refresh token was expired. Please make a new signin request"
            );
        }

        return token;
    }

    /**
     * Deletes all refresh tokens for a specific user.
     *
     * @param userId the ID of the user whose tokens should be deleted
     * @return the number of deleted tokens
     */
    public int deleteByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        int deletedCount = refreshTokenRepository.deleteByUser(user);
        logger.debug("Deleted {} refresh token(s) for user: {}", deletedCount, user.getUsername());

        return deletedCount;
    }
}
