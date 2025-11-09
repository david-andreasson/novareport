package com.novareport.payments_xmr_service.service;

/**
 * Exception thrown when an invalid subscription plan is provided.
 */
public class InvalidPlanException extends RuntimeException {
    public InvalidPlanException(String message) {
        super(message);
    }
}
