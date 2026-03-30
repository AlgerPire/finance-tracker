package com.finance_tracker.backend_server.account.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AccountListResponse(
        List<AccountResponse> accounts,
        String message
) {}
