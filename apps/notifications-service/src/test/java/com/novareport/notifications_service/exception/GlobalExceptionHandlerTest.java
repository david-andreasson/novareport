package com.novareport.notifications_service.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleNoSuchElementReturnsNotFoundProblem() {
        NoSuchElementException ex = new NoSuchElementException("missing");

        ProblemDetail problem = handler.handleNoSuchElement(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getTitle()).isEqualTo("Resource Not Found");
        assertThat(problem.getDetail()).isEqualTo("missing");
    }

    @Test
    void handleIllegalArgumentReturnsBadRequestProblem() {
        IllegalArgumentException ex = new IllegalArgumentException("bad arg");

        ProblemDetail problem = handler.handleIllegalArgument(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Invalid Request");
        assertThat(problem.getDetail()).isEqualTo("bad arg");
    }

    @Test
    void handleValidationExceptionBuildsDetailFromFieldErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError error1 = new FieldError("object", "field1", "msg1");
        FieldError error2 = new FieldError("object", "field2", "msg2");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(error1, error2));
        when(ex.getMessage()).thenReturn("Validation failed");

        ProblemDetail problem = handler.handleValidationException(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Validation Failed");
        assertThat(problem.getDetail())
                .contains("field1: msg1")
                .contains("field2: msg2");
    }

    @Test
    void handleGenericExceptionReturnsInternalServerError() {
        Exception ex = new RuntimeException("boom");

        ProblemDetail problem = handler.handleGenericException(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getTitle()).isEqualTo("Internal Server Error");
        assertThat(problem.getDetail()).isEqualTo("An unexpected error occurred");
    }
}
