package com.novareport.payments_stripe_service.service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import org.springframework.stereotype.Component;

/**
 * Default implementation that delegates directly to Stripe's PaymentIntent API.
 */
@Component
public class DefaultStripePaymentClient implements StripePaymentClient {

    @Override
    public PaymentIntent createPaymentIntent(PaymentIntentCreateParams params) throws StripeException {
        return PaymentIntent.create(params);
    }
}
