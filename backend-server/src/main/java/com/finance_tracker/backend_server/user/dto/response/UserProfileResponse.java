package com.finance_tracker.backend_server.user.dto.response;

import java.util.Set;

/**
 * DTO representing the authenticated user's own profile details.
 */
public record UserProfileResponse(
        String username,
        String email,
        Set<String> roles
) {
}
