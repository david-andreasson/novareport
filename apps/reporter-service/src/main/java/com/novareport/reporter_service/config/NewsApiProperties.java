package com.novareport.reporter_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "newsapi")
public record NewsApiProperties(
    @DefaultValue("false") boolean enabled,
    @DefaultValue("20") int maxResults
) {
}
