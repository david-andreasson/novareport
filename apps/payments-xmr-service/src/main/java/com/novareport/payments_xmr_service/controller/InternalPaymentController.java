package com.novareport.payments_xmr_service.controller;

import com.novareport.payments_xmr_service.service.PaymentService;
import com.novareport.payments_xmr_service.util.LogSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/internal/payments")
@RequiredArgsConstructor
@Slf4j
public class InternalPaymentController {

    private final PaymentService paymentService;

    @PostMapping("/{paymentId}/confirm")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void confirmPayment(@PathVariable UUID paymentId) {
        log.info("Confirming payment {}", LogSanitizer.sanitize(paymentId));
        paymentService.confirmPayment(paymentId);
    }
}
