package com.finance_tracker.backend_server.user.entity.enumeration;

public enum ERole {

    /**
     * Standard user role with basic access permissions.
     * <p>
     * Default role assigned to newly registered users.
     * </p>
     */
    ROLE_USER,

    /**
     * Administrator role with elevated system permissions.
     * <p>
     * Can manage users and system settings in addition to moderator permissions.
     * </p>
     */
    ROLE_ADMIN,
}
