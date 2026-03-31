package com.finance_tracker.backend_server.user.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserListResponse(
        List<UserSummaryResponse> users,
        String message
) {
}
