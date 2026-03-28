package com.finance_tracker.backend_server.common.exception;

import java.io.Serial;

public class InvalidTransactionException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public InvalidTransactionException(String message) {
        super(message);
    }
}