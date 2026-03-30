package com.finance_tracker.backend_server.transaction.dto.response;


import com.finance_tracker.backend_server.transaction.entity.enumeration.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
        Long id,
        TransactionType type,
        BigDecimal amount,
        Long accountId,
        Long sourceAccountId,
        Long targetAccountId,
        BigDecimal accountBalanceAfter,
        String description,
        Instant transactionAt,
        Instant createdAt
) {
}