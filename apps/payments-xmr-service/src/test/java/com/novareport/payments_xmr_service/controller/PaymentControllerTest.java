package com.novareport.payments_xmr_service.controller;

import com.novareport.payments_xmr_service.dto.CreatePaymentRequest;
import com.novareport.payments_xmr_service.dto.CreatePaymentResponse;
import com.novareport.payments_xmr_service.dto.PaymentStatusResponse;
import com.novareport.payments_xmr_service.domain.PaymentStatus;
import com.novareport.payments_xmr_service.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.math.BigDecimal;
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
    void createPaymentDelegatesToServiceUsingUserIdFromRequest() {
        UUID userId = UUID.randomUUID();
        CreatePaymentRequest requestBody = new CreatePaymentRequest("monthly", BigDecimal.ONE);
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.setAttribute("uid", userId.toString());

        CreatePaymentResponse expectedResponse = new CreatePaymentResponse(UUID.randomUUID(), "addr", BigDecimal.ONE, Instant.now());
        when(paymentService.createPayment(eq(userId), eq("monthly"), eq(BigDecimal.ONE))).thenReturn(expectedResponse);

        CreatePaymentResponse response = controller.createPayment(requestBody, httpRequest);

        assertThat(response).isEqualTo(expectedResponse);
        verify(paymentService).createPayment(userId, "monthly", BigDecimal.ONE);
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
