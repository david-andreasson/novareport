package com.novareport.reporter_service;

import com.novareport.reporter_service.config.ReporterProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(ReporterProperties.class)
@EnableScheduling
public class ReporterServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReporterServiceApplication.class, args);
	}

}
