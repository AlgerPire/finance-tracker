package com.finance_tracker.backend_server.account.dto;

import com.finance_tracker.backend_server.account.entity.enumeration.AccountType;
import com.finance_tracker.backend_server.account.entity.enumeration.CurrencyType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateAccountRequest(
        @NotNull AccountType accountType,
        @NotNull
        @DecimalMin(value = "0.00", inclusive = true)
        @Digits(integer = 17, fraction = 2)
        BigDecimal initialBalance,
        @NotNull CurrencyType currencyType
) {}
