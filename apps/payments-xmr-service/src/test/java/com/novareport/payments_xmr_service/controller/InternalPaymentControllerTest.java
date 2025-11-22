package com.novareport.payments_xmr_service.controller;

import com.novareport.payments_xmr_service.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InternalPaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private InternalPaymentController controller;

    @Test
    void confirmPaymentDelegatesToService() {
        UUID paymentId = UUID.randomUUID();

        controller.confirmPayment(paymentId);

        verify(paymentService).confirmPayment(paymentId);
    }
}
