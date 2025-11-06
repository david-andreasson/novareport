package com.novareport.payments_xmr_service.controller;

import com.novareport.payments_xmr_service.dto.CreatePaymentRequest;
import com.novareport.payments_xmr_service.dto.CreatePaymentResponse;
import com.novareport.payments_xmr_service.dto.PaymentStatusResponse;
import com.novareport.payments_xmr_service.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public CreatePaymentResponse createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            HttpServletRequest httpRequest
    ) {
        UUID userId = resolveUserId(httpRequest);
        log.info("Creating payment for user {}", userId);

        return paymentService.createPayment(
                userId,
                request.plan(),
                request.amountXmr()
        );
    }

    @GetMapping("/{paymentId}/status")
    public PaymentStatusResponse getPaymentStatus(
            @PathVariable UUID paymentId,
            HttpServletRequest httpRequest
    ) {
        UUID userId = resolveUserId(httpRequest);
        log.debug("Getting payment status for payment {} and user {}", paymentId, userId);

        return paymentService.getPaymentStatus(paymentId, userId);
    }

    private UUID resolveUserId(HttpServletRequest request) {
        Object uidAttr = request.getAttribute("uid");
        if (uidAttr instanceof String value) {
            try {
                return UUID.fromString(value);
            } catch (IllegalArgumentException e) {
                log.error("Invalid UUID in uid attribute: {}", value);
                throw new IllegalStateException("Invalid user ID");
            }
        }
        throw new IllegalStateException("User ID not found in request");
    }
}
