package com.finance_tracker.backend_server.transaction.dto.response;


import com.finance_tracker.backend_server.transaction.entity.enumeration.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionListResponse(
        Long id,
        TransactionType type,
        BigDecimal amount,
        String currencyType,
        Long accountId,
        Long sourceAccountId,
        Long targetAccountId,
        String sourceAccountName,
        String sourceAccountIdentification,
        String targetAccountName,
        String targetAccountIdentification,
        String description,
        Instant transactionAt,
        Instant createdAt
) {
}
