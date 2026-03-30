package com.finance_tracker.backend_server.account.dto.response;

import com.finance_tracker.backend_server.account.entity.enumeration.AccountType;
import com.finance_tracker.backend_server.account.entity.enumeration.CurrencyType;

import java.math.BigDecimal;
import java.time.Instant;

public record AdminAccountResponse(
        Long id,
        String accountIdentification,
        String ownerUsername,
        AccountType accountType,
        BigDecimal balance,
        CurrencyType currencyType,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
