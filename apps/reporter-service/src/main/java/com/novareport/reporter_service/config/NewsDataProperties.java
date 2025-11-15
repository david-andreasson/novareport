package com.novareport.reporter_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "newsapi.newsdata")
public record NewsDataProperties(
    @DefaultValue("false") boolean enabled,
    String baseUrl,
    String apiKey,
    @DefaultValue("24") int timeframeHours
) {
    public String resolvedBaseUrl() {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://newsdata.io/api/1";
        }
        return baseUrl;
    }

    public int resolvedTimeframeHours() {
        if (timeframeHours < 1) {
            return 1;
        }
        if (timeframeHours > 48) {
            return 48;
        }
        return timeframeHours;
    }
}
