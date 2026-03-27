package com.finance_tracker.backend_server.user.repository;

import com.finance_tracker.backend_server.user.entity.User;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

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
}
