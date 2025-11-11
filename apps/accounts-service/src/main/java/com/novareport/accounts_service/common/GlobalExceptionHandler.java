package com.novareport.accounts_service.common;

import com.novareport.accounts_service.common.exception.AccountsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ProblemDetail handleValidation(Exception ex) {
        List<FieldError> errors = ex instanceof MethodArgumentNotValidException manv
            ? manv.getBindingResult().getFieldErrors()
            : ((BindException) ex).getBindingResult().getFieldErrors();

        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        detail.setTitle("Validation failed");
        detail.setDetail("Request validation failed");
        detail.setProperty("errors", errors.stream()
            .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage)));
        return detail;
    }

    @ExceptionHandler(AccountsException.class)
    public ProblemDetail handleAccountsException(AccountsException ex) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.valueOf(ex.getStatus().value()));
        detail.setTitle(ex.getClass().getSimpleName());
        detail.setDetail(ex.getMessage());
        return detail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        ProblemDetail detail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        detail.setTitle("Internal Server Error");
        detail.setDetail("An unexpected error occurred");
        detail.setProperty("error", ex.getClass().getSimpleName());
        return detail;
    }
}
