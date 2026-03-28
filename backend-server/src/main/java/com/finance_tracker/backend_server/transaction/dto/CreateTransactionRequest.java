package com.finance_tracker.backend_server.transaction.dto;

import com.finance_tracker.backend_server.transaction.entity.enumeration.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateTransactionRequest(
        @NotNull TransactionType type,
        @NotNull
        @DecimalMin(value = "0.01", inclusive = true)
        @Digits(integer = 17, fraction = 2)
        BigDecimal amount,
        Long accountId,
        Long sourceAccountId,
        Long targetAccountId,
        String description,
        Instant transactionAt
) {
}
