package com.novareport.payments_xmr_service.service;

public class InvalidPaymentStateException extends RuntimeException {

    public InvalidPaymentStateException(String message) {
        super(message);
    }
}
