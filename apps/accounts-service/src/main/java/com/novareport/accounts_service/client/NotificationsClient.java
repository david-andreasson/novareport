package com.novareport.accounts_service.client;

import com.novareport.accounts_service.util.LogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Objects;

@Component
public class NotificationsClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationsClient.class);

    private final RestClient restClient;
    private final String internalKey;

    public NotificationsClient(
            RestClient.Builder builder,
            @Value("${notifications.base-url:http://notifications-service:8080}") String baseUrl,
            @Value("${internal.api-key:}") String internalKey
    ) {
        this.restClient = builder.baseUrl(Objects.requireNonNull(baseUrl, "notifications.base-url must not be null")).build();
        this.internalKey = internalKey;
    }

    public void sendWelcomeEmail(String email, String firstName) {
        if (internalKey == null || internalKey.isBlank()) {
            log.warn("Internal API key is not configured; skipping welcome email for {}", LogSanitizer.sanitize(email));
            return;
        }

        WelcomeEmailRequest request = new WelcomeEmailRequest(email, firstName);
        try {
            restClient.post()
                    .uri("/api/v1/internal/notifications/welcome-email")
                    .header("X-INTERNAL-KEY", internalKey)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn(
                    "Failed to send welcome email for {}: {}",
                    LogSanitizer.sanitize(email),
                    LogSanitizer.sanitize(ex.getMessage())
            );
        }
    }

    private record WelcomeEmailRequest(String email, String firstName) {
    }
}
