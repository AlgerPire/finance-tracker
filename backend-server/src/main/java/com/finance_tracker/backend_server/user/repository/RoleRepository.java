package com.finance_tracker.backend_server.user.repository;

import com.finance_tracker.backend_server.user.entity.Role;
import com.finance_tracker.backend_server.user.entity.enumeration.ERole;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<@NonNull Role, @NonNull Integer> {

    /**
     * Finds a role by its name enumeration value.
     *
     * @param name the role enum value to search for
     * @return an Optional containing the role if found, empty otherwise
     */
    Optional<Role> findByName(ERole name);
}