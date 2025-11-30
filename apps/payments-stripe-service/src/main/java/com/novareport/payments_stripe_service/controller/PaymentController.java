package com.novareport.payments_stripe_service.controller;

import com.novareport.payments_stripe_service.dto.CreatePaymentIntentRequest;
import com.novareport.payments_stripe_service.dto.CreatePaymentIntentResponse;
import com.novareport.payments_stripe_service.dto.PaymentStatusResponse;
import com.novareport.payments_stripe_service.service.PaymentService;
import com.novareport.payments_stripe_service.util.LogSanitizer;
import com.novareport.payments_stripe_service.util.RequestUtils;
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
@RequestMapping("/api/v1/payments-stripe")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-intent")
    @ResponseStatus(HttpStatus.CREATED)
    public CreatePaymentIntentResponse createPaymentIntent(
            @Valid @RequestBody CreatePaymentIntentRequest request,
            HttpServletRequest httpRequest
    ) {
        UUID userId = RequestUtils.resolveUserId(httpRequest);
        log.info("Creating Stripe payment intent for user {} with plan {}",
                LogSanitizer.sanitize(userId), LogSanitizer.sanitize(request.plan()));

        return paymentService.createPaymentIntent(userId, request.plan());
    }

    @GetMapping("/{paymentId}/status")
    public PaymentStatusResponse getPaymentStatus(
            @PathVariable UUID paymentId,
            HttpServletRequest httpRequest
    ) {
        UUID userId = RequestUtils.resolveUserId(httpRequest);
        log.debug("Getting Stripe payment status for payment {} and user {}",
                LogSanitizer.sanitize(paymentId), LogSanitizer.sanitize(userId));

        return paymentService.getPaymentStatus(paymentId, userId);
    }
}
