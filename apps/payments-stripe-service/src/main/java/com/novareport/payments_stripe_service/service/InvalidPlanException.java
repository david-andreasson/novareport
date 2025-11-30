package com.novareport.payments_stripe_service.service;

public class InvalidPlanException extends RuntimeException {

    public InvalidPlanException(String message) {
        super(message);
    }
}
