package com.novareport.payments_stripe_service.dto;

import java.util.UUID;

public record CreatePaymentIntentResponse(
        UUID paymentId,
        String clientSecret,
        Long amountFiat,
        String currencyFiat
) {
}
