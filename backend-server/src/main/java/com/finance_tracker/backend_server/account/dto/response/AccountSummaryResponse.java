package com.finance_tracker.backend_server.account.dto.response;

import com.finance_tracker.backend_server.account.entity.enumeration.AccountType;
import com.finance_tracker.backend_server.account.entity.enumeration.CurrencyType;

import java.time.Instant;

public record AccountSummaryResponse(
        Long id,
        String accountIdentification,
        AccountType accountType,
        CurrencyType currencyType,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
