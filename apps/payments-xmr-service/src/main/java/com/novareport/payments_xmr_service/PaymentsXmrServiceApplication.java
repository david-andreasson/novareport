package com.novareport.payments_xmr_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class PaymentsXmrServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaymentsXmrServiceApplication.class, args);
	}

}
