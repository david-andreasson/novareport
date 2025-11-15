package com.novareport.reporter_service;

import com.novareport.reporter_service.config.ReporterProperties;
import com.novareport.reporter_service.config.NewsApiProperties;
import com.novareport.reporter_service.config.NewsDataProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties({ReporterProperties.class, NewsApiProperties.class, NewsDataProperties.class})
@EnableScheduling
public class ReporterServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReporterServiceApplication.class, args);
	}

}
