package com.novareport.payments_stripe_service.service;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;

/**
 * Small abstraction over the Stripe Java client to make PaymentService easier to unit test.
 */
public interface StripePaymentClient {

    PaymentIntent createPaymentIntent(PaymentIntentCreateParams params) throws StripeException;
}
