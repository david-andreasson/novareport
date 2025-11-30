package com.novareport.payments_stripe_service.service;

public class SubscriptionActivationException extends RuntimeException {

    public SubscriptionActivationException(String message) {
        super(message);
    }

    public SubscriptionActivationException(String message, Throwable cause) {
        super(message, cause);
    }
}
