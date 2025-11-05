package com.novareport.accounts_service.common.exception;

import org.springframework.http.HttpStatus;

public class EmailAlreadyExistsException extends AccountsException {

    public EmailAlreadyExistsException(String email) {
        super("Email already registered: " + email, HttpStatus.CONFLICT);
    }
}
