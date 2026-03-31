package com.finance_tracker.backend_server.user.dto.response;

/**
 * Lightweight user projection exposing only public-safe fields.
 */
public record UserSummaryResponse(
        Long id,
        String username,
        String email
) {
}
