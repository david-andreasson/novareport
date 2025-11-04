package com.novareport.reporter_service.client;

import com.novareport.reporter_service.domain.SubscriptionAccessResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class SubscriptionsClient {

    private final WebClient webClient;

    public SubscriptionsClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<SubscriptionAccessResponse> hasAccess(String baseUrl, String bearerToken) {
        return webClient
            .get()
            .uri(baseUrl + "/api/v1/subscriptions/me/has-access")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
            .retrieve()
            .bodyToMono(SubscriptionAccessResponse.class);
    }
}
