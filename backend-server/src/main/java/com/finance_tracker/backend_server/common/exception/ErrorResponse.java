package com.finance_tracker.backend_server.common.exception;

import java.time.Instant;

public record ErrorResponse(
        int status,
        Instant timestamp,
        String error,
        String message
) {}
