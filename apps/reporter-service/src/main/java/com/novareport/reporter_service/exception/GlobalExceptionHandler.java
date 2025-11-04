package com.novareport.reporter_service.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException exception) {
        HttpStatus status = exception.getStatusCode() instanceof HttpStatus httpStatus ? httpStatus : HttpStatus.BAD_REQUEST;
        ApiError body = new ApiError(status, status.getReasonPhrase(), exception.getReason());
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getAllErrors().stream()
            .findFirst()
            .map(error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : error.getCode())
            .orElse("Validation error");
        ApiError body = new ApiError(HttpStatus.BAD_REQUEST, "Validation error", message);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException exception) {
        ApiError body = new ApiError(HttpStatus.BAD_REQUEST, "Bad request", exception.getMessage());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnknown(Exception exception) {
        log.error("Unexpected error", exception);
        ApiError body = new ApiError(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error", "Something went wrong");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
