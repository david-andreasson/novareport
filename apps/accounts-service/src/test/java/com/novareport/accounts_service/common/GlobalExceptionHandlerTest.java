package com.novareport.accounts_service.common;

import com.novareport.accounts_service.common.exception.EmailAlreadyExistsException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleValidationForMethodArgumentNotValidBuildsErrorMap() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError error1 = new FieldError("object", "email", "must be valid");
        FieldError error2 = new FieldError("object", "password", "too short");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(error1, error2));

        ProblemDetail detail = handler.handleValidation(ex);

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(detail.getTitle()).isEqualTo("Validation failed");
        assertThat(detail.getDetail()).isEqualTo("Request validation failed");

        Map<String, Object> props = detail.getProperties();
        if (props == null) {
            throw new IllegalStateException("ProblemDetail properties must not be null");
        }

        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) props.get("errors");
        assertThat(errors)
                .containsEntry("email", "must be valid")
                .containsEntry("password", "too short");
    }

    @Test
    void handleValidationForBindExceptionBuildsErrorMap() {
        BindException ex = mock(BindException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError error = new FieldError("object", "field", "invalid");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(error));

        ProblemDetail detail = handler.handleValidation(ex);

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void handleAccountsExceptionUsesStatusAndMessage() {
        EmailAlreadyExistsException ex = new EmailAlreadyExistsException("taken@example.com");

        ProblemDetail detail = handler.handleAccountsException(ex);

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(detail.getTitle()).isEqualTo("EmailAlreadyExistsException");
        assertThat(detail.getDetail()).contains("taken@example.com");
    }

    @Test
    void handleUnexpectedReturnsInternalServerError() {
        Exception ex = new RuntimeException("boom");

        ProblemDetail detail = handler.handleUnexpected(ex);

        assertThat(detail.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(detail.getTitle()).isEqualTo("Internal Server Error");
        assertThat(detail.getDetail()).isEqualTo("An unexpected error occurred");
        assertThat(detail.getProperties()).containsKey("error");
    }
}
