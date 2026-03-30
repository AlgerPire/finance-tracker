package com.finance_tracker.backend_server.transaction.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record TransferRequest(
        @NotBlank
        @Size(min = 6, max = 6, message = "accountIdentification must be exactly 6 characters")
        String sourceAccountIdentification,

        @NotBlank
        @Size(min = 6, max = 6, message = "accountIdentification must be exactly 6 characters")
        String targetAccountIdentification,

        @NotNull
        @DecimalMin(value = "0.01", inclusive = true)
        @Digits(integer = 17, fraction = 2)
        BigDecimal amount,

        String description,
        Instant transactionAt
) {
}
