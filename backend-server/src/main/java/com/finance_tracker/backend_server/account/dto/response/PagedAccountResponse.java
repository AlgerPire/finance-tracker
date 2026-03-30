package com.finance_tracker.backend_server.account.dto.response;

import java.util.List;

public record PagedAccountResponse(
        List<AdminAccountResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}
