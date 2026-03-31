package com.finance_tracker.backend_server.user.repository;

import com.finance_tracker.backend_server.user.entity.User;
import com.finance_tracker.backend_server.user.entity.enumeration.ERole;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for {@link User} entity operations.
 * <p>
 * Provides database access methods for user management including
 * finding users by username or email, checking if a user exists,
 * and finding users with a specific role.
 * </p>
 *
 * @author Finance Tracker Team
 * @version 1.0
 * @since 2026
 */
@Repository
public interface UserRepository extends JpaRepository<@NonNull User, @NonNull Long> {

    /**
     * Finds a user by their username.
     *
     * @param username the username to search for
     * @return an Optional containing the user if found, empty otherwise
     */
    Optional<User> findByUsername(String username);

    /**
     * Checks if a user with the given username already exists.
     *
     * @param username the username to check
     * @return true if a user with the username exists, false otherwise
     */
    Boolean existsByUsername(String username);

    /**
     * Checks if a user with the given email already exists.
     *
     * @param email the email to check
     * @return true if a user with the email exists, false otherwise
     */
    Boolean existsByEmail(String email);

    /**
     * Finds all users with a specific role.
     *
     * @param role the role to search for
     * @param pageable the pageable object
     * @return a page of users with the specific role
     */
    @EntityGraph(attributePaths = {"roles"})
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :role")
    Page<User> findAllWithRoles(
            @Param("role") ERole role, Pageable pageable);

    /**
     * Returns all users that do not have the given role and are not the excluded user.
     *
     * @param excludedRole  role to filter out (e.g. ROLE_ADMIN)
     * @param excludedUserId id of the user to exclude (e.g. the caller)
     * @return list of matching users
     */
    @EntityGraph(attributePaths = {"roles"})
    @Query("""
            SELECT u FROM User u
            WHERE u.id <> :excludedUserId
              AND NOT EXISTS (
                  SELECT r FROM u.roles r WHERE r.name = :excludedRole
              )
            """)
    List<User> findAllExcludingRoleAndUser(
            @Param("excludedRole") ERole excludedRole,
            @Param("excludedUserId") Long excludedUserId);
}
