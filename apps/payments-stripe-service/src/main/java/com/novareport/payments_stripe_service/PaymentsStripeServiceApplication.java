package com.novareport.payments_stripe_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class PaymentsStripeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentsStripeServiceApplication.class, args);
    }
}
