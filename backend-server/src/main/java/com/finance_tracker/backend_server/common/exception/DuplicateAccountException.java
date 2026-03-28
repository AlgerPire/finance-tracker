package com.finance_tracker.backend_server.common.exception;

import java.io.Serial;

public class DuplicateAccountException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public DuplicateAccountException(String message) {
        super(message);
    }
}
