package com.novareport.notifications_service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Disabled("Disabled to avoid starting full Spring context (Flyway, Mail, Jobs) in unit test suite")
class NotificationsServiceApplicationMainTest {

    @Test
    void mainStartsApplicationWithoutErrors() {
        assertDoesNotThrow(() -> NotificationsServiceApplication.main(new String[]{
                "--spring.main.web-application-type=none",
                "--spring.flyway.enabled=false",
                "--jwt.secret=01234567890123456789012345678901",
                "--jwt.issuer=test-issuer",
                "--notifications.email.from=test@example.com"
        }));
    }
}
