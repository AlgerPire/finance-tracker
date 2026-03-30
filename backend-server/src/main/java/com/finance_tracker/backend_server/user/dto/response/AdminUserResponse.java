package com.finance_tracker.backend_server.user.dto.response;

import java.util.Set;

public record AdminUserResponse(
        Long id,
        String username,
        String email,
        Set<String> roles
) {
}
