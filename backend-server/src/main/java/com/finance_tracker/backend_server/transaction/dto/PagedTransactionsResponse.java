package com.finance_tracker.backend_server.transaction.dto;

import java.util.List;

public record PagedTransactionsResponse(
        List<TransactionListResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
}