package com.novareport.reporter_service.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleResponseStatusUsesStatusAndReason() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.FORBIDDEN, "Denied");

        ProblemDetail problem = handler.handleResponseStatus(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(problem.getTitle()).isEqualTo("Request Error");
        assertThat(problem.getDetail()).isEqualTo("Denied");
    }

    @Test
    void handleResponseStatusUsesDefaultDetailWhenReasonNull() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.BAD_REQUEST);

        ProblemDetail problem = handler.handleResponseStatus(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getDetail()).isEqualTo("Request failed");
    }

    @Test
    void handleValidationBuildsDetailFromFieldErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError error1 = new FieldError("object", "field1", "msg1");
        FieldError error2 = new FieldError("object", "field2", "msg2");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(error1, error2));

        ProblemDetail problem = handler.handleValidation(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Validation Failed");
        assertThat(problem.getDetail())
            .contains("field1: msg1")
            .contains("field2: msg2");
    }

    @Test
    void handleIllegalArgumentReturnsBadRequest() {
        IllegalArgumentException ex = new IllegalArgumentException("bad");

        ProblemDetail problem = handler.handleIllegalArgument(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Invalid Request");
        assertThat(problem.getDetail()).isEqualTo("bad");
    }

    @Test
    void handleUnknownReturnsInternalServerError() {
        Exception ex = new RuntimeException("boom");

        ProblemDetail problem = handler.handleUnknown(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getTitle()).isEqualTo("Internal Server Error");
        assertThat(problem.getDetail()).isEqualTo("An unexpected error occurred");
    }
}
