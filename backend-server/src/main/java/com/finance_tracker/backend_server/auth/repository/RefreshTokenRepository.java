package com.finance_tracker.backend_server.auth.repository;

import com.finance_tracker.backend_server.auth.entity.RefreshToken;
import com.finance_tracker.backend_server.user.entity.User;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for {@link RefreshToken} entity operations.
 * <p>
 * Provides database access methods for refresh token management including
 * finding tokens by their string value or associated user, and deleting
 * tokens by user.
 * </p>
 *
 * @author FullSecurity Team
 * @version 1.0
 * @see RefreshToken
 * @see com.finance_tracker.backend_server.user.entity.User
 * @since 2024
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<@NonNull RefreshToken, @NonNull Long> {

    /**
     * Finds a refresh token by its token string.
     *
     * @param token the token string to search for
     * @return an Optional containing the refresh token if found, empty otherwise
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Finds a refresh token by its associated user.
     *
     * @param user the user to search for
     * @return an Optional containing the refresh token if found, empty otherwise
     */
    Optional<RefreshToken> findByUser(User user);

    /**
     * Deletes all refresh tokens associated with a user.
     * <p>
     * This method is used during logout or when creating a new refresh token
     * to ensure only one active token exists per user.
     * </p>
     *
     * @param user the user whose tokens should be deleted
     * @return the number of deleted tokens
     */
    @Modifying
    int deleteByUser(User user);
}
