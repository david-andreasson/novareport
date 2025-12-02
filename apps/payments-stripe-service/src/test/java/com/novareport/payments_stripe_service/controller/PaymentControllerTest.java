package com.novareport.payments_stripe_service.controller;

import com.novareport.payments_stripe_service.domain.PaymentStatus;
import com.novareport.payments_stripe_service.dto.CreatePaymentIntentRequest;
import com.novareport.payments_stripe_service.dto.CreatePaymentIntentResponse;
import com.novareport.payments_stripe_service.dto.PaymentStatusResponse;
import com.novareport.payments_stripe_service.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController controller;

    @Test
    void createPaymentIntentDelegatesToServiceUsingUserIdFromRequest() {
        UUID userId = UUID.randomUUID();
        CreatePaymentIntentRequest requestBody = new CreatePaymentIntentRequest("monthly");
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setAttribute("uid", userId.toString());

        CreatePaymentIntentResponse expectedResponse = new CreatePaymentIntentResponse(UUID.randomUUID(), "cs_test", 49_00L, "SEK");
        when(paymentService.createPaymentIntent(eq(userId), eq("monthly"))).thenReturn(expectedResponse);

        CreatePaymentIntentResponse response = controller.createPaymentIntent(requestBody, httpRequest);

        assertThat(response).isEqualTo(expectedResponse);
        verify(paymentService).createPaymentIntent(userId, "monthly");
    }

    @Test
    void getPaymentStatusDelegatesToServiceWithUserIdFromRequest() {
        UUID userId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setAttribute("uid", userId.toString());

        PaymentStatusResponse expected = new PaymentStatusResponse(paymentId, PaymentStatus.CONFIRMED, Instant.now(), Instant.now());
        when(paymentService.getPaymentStatus(paymentId, userId)).thenReturn(expected);

        PaymentStatusResponse response = controller.getPaymentStatus(paymentId, httpRequest);

        assertThat(response).isEqualTo(expected);
        verify(paymentService).getPaymentStatus(paymentId, userId);
    }
}
