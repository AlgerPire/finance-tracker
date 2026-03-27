package com.finance_tracker.backend_server.user.service;

import com.finance_tracker.backend_server.user.entity.Role;
import com.finance_tracker.backend_server.user.entity.enumeration.ERole;

import java.util.List;

/**
 * Service interface for role management operations.
 * <p>
 * This interface defines the contract for role-related operations including
 * finding roles by name, retrieving all roles, and initializing default roles.
 * </p>
 *
 * @author FullSecurity Team
 * @version 1.0
 * @since 2024
 */
public interface RoleService {

    /**
     * Finds a role by its name.
     *
     * @param roleName the role enumeration value to search for
     * @return the {@link com.finance_tracker.backend_server.user.entity.Role} entity matching the name
     * @throws IllegalArgumentException if role is not found
     */
    Role findByName(ERole roleName);

    /**
     * Retrieves all roles from the database.
     *
     * @return a list of all {@link Role} entities
     */
    List<Role> findAll();

    /**
     * Checks if a role exists by its name.
     *
     * @param roleName the role enumeration value to check
     * @return true if the role exists, false otherwise
     */
    boolean existsByName(ERole roleName);

    /**
     * Initializes default roles in the database.
     * <p>
     * This method creates all roles defined in {@link ERole} enum
     * if they don't already exist in the database.
     * </p>
     */
    void initializeRoles();

    /**
     * Creates a new role in the database.
     *
     * @param roleName the role enumeration value to create
     * @return the created {@link Role} entity
     */
    Role createRole(ERole roleName);
}
