package com.finance_tracker.backend_server.user.dto.response;

import java.util.List;

public record PagedUserResponse(
        List<AdminUserResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
