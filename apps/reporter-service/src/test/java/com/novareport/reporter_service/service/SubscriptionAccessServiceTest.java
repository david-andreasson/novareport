package com.novareport.reporter_service.service;

import com.novareport.reporter_service.client.SubscriptionsClient;
import com.novareport.reporter_service.domain.SubscriptionAccessResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("null")
class SubscriptionAccessServiceTest {

    private SubscriptionsClient subscriptionsClient;
    private SubscriptionAccessService service;

    @BeforeEach
    void setUp() {
        subscriptionsClient = mock(SubscriptionsClient.class);
        service = new SubscriptionAccessService(subscriptionsClient, "http://subs");
    }

    @Test
    void assertAccessThrowsUnauthorizedWhenHeaderMissingOrInvalid() {
        assertThatThrownBy(() -> service.assertAccess(null))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Missing bearer token");

        assertThatThrownBy(() -> service.assertAccess("Basic abc"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Missing bearer token");

        assertThatThrownBy(() -> service.assertAccess("Bearer "))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Missing bearer token");
    }

    @Test
    void assertAccessReturnsWhenSubscriptionHasAccess() {
        when(subscriptionsClient.hasAccess("http://subs", "token"))
            .thenReturn(Mono.just(new SubscriptionAccessResponse(true)));

        assertThatCode(() -> service.assertAccess("Bearer token"))
            .doesNotThrowAnyException();
    }

    @Test
    void assertAccessThrowsForbiddenWhenSubscriptionHasNoAccess() {
        when(subscriptionsClient.hasAccess("http://subs", "token"))
            .thenReturn(Mono.just(new SubscriptionAccessResponse(false)));

        assertThatThrownBy(() -> service.assertAccess("Bearer token"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Subscription required");
    }

    @Test
    void assertAccessTreats4xxFromSubscriptionsAsNoAccess() {
        WebClientResponseException ex = WebClientResponseException.create(
            404,
            "Not Found",
            HttpHeaders.EMPTY,
            new byte[0],
            StandardCharsets.UTF_8
        );
        when(subscriptionsClient.hasAccess(anyString(), anyString()))
            .thenReturn(Mono.error(ex));

        assertThatThrownBy(() -> service.assertAccess("Bearer token"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Subscription required");
    }

    @Test
    void assertAccessWraps5xxFromSubscriptionsAsBadGateway() {
        WebClientResponseException ex = WebClientResponseException.create(
            500,
            "Server Error",
            HttpHeaders.EMPTY,
            new byte[0],
            StandardCharsets.UTF_8
        );
        when(subscriptionsClient.hasAccess(anyString(), anyString()))
            .thenReturn(Mono.error(ex));

        assertThatThrownBy(() -> service.assertAccess("Bearer token"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Failed to verify subscription access");
    }

    @Test
    void assertAccessWrapsUnexpectedExceptionsAsBadGateway() {
        when(subscriptionsClient.hasAccess(anyString(), anyString()))
            .thenReturn(Mono.error(new RuntimeException("boom")));

        assertThatThrownBy(() -> service.assertAccess("Bearer token"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Failed to verify subscription access");
    }
}
