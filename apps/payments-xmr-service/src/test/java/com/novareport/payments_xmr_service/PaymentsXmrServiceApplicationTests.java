package com.novareport.payments_xmr_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@org.springframework.test.context.ActiveProfiles("test")
@org.springframework.context.annotation.Import(com.novareport.payments_xmr_service.config.TestRestTemplateConfig.class)
class PaymentsXmrServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
