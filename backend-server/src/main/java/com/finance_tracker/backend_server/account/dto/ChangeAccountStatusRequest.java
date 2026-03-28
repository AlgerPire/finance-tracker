package com.finance_tracker.backend_server.account.dto;

import jakarta.validation.constraints.NotNull;

public record ChangeAccountStatusRequest(
        @NotNull Boolean active
) {
}
