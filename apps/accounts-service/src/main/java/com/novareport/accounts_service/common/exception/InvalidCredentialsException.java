package com.novareport.accounts_service.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends AccountsException {

    private InvalidCredentialsException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }

    public static InvalidCredentialsException invalidCredentials() {
        return new InvalidCredentialsException("Invalid credentials");
    }
}
