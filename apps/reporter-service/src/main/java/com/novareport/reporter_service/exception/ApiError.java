package com.novareport.reporter_service.exception;

import org.springframework.http.HttpStatus;

public record ApiError(HttpStatus status, String error, String message) {
}
