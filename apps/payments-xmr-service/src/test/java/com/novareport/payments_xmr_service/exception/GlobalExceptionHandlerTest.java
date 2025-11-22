package com.novareport.payments_xmr_service.exception;

import com.novareport.payments_xmr_service.service.InvalidPaymentStateException;
import com.novareport.payments_xmr_service.service.InvalidPlanException;
import com.novareport.payments_xmr_service.service.PaymentNotFoundException;
import com.novareport.payments_xmr_service.service.SubscriptionActivationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.BindingResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handlePaymentNotFoundReturnsNotFoundProblemDetail() {
        ProblemDetail problem = handler.handlePaymentNotFound(new PaymentNotFoundException("not found"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getTitle()).isEqualTo("Payment Not Found");
    }

    @Test
    void handleInvalidPaymentStateReturnsConflictProblemDetail() {
        ProblemDetail problem = handler.handleInvalidPaymentState(new InvalidPaymentStateException("invalid"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problem.getTitle()).isEqualTo("Invalid Payment State");
    }

    @Test
    void handleInvalidPlanReturnsBadRequestProblemDetail() {
        ProblemDetail problem = handler.handleInvalidPlan(new InvalidPlanException("bad plan"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Invalid Plan");
    }

    @Test
    void handleSubscriptionActivationFailureReturnsInternalServerErrorProblemDetail() {
        ProblemDetail problem = handler.handleSubscriptionActivationFailure(new SubscriptionActivationException("fail"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getTitle()).isEqualTo("Subscription Activation Failed");
    }

    @Test
    void handleValidationExceptionReturnsBadRequestProblemDetail() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());
        ProblemDetail problem = handler.handleValidationException(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Validation Failed");
    }

    @Test
    void handleIllegalArgumentReturnsBadRequestProblemDetail() {
        ProblemDetail problem = handler.handleIllegalArgument(new IllegalArgumentException("bad"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Invalid Request");
    }

    @Test
    void handleGenericExceptionReturnsInternalServerErrorProblemDetail() {
        ProblemDetail problem = handler.handleGenericException(new RuntimeException("boom"));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getTitle()).isEqualTo("Internal Server Error");
    }
}
