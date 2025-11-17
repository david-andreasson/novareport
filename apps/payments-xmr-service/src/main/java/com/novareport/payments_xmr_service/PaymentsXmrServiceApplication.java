package com.novareport.payments_xmr_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableRetry
@EnableScheduling
public class PaymentsXmrServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaymentsXmrServiceApplication.class, args);
	}

}
