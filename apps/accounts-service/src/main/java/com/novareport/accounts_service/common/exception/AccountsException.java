package com.novareport.accounts_service.common.exception;

import org.springframework.http.HttpStatus;

public abstract class AccountsException extends RuntimeException {

    private final HttpStatus status;

    protected AccountsException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
