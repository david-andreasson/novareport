package com.novareport.reporter_service;

import com.novareport.reporter_service.config.ReporterProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ReporterProperties.class)
public class ReporterServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReporterServiceApplication.class, args);
	}

}
