package com.novareport.accounts_service.util;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LogSanitizerTest {

    @Test
    void sanitizeStringRemovesNewlines() {
        String input = "test\nvalue\rwith\r\nnewlines";
        String result = LogSanitizer.sanitize(input);
        assertThat(result).doesNotContain("\n", "\r");
    }

    @Test
    void sanitizeStringHandlesNull() {
        String result = LogSanitizer.sanitize((String) null);
        assertThat(result).isEqualTo("null");
    }

    @Test
    void sanitizeObjectHandlesNull() {
        String result = LogSanitizer.sanitize((Object) null);
        assertThat(result).isEqualTo("null");
    }

    @Test
    void sanitizeObjectConvertsToString() {
        UUID uuid = UUID.randomUUID();
        String result = LogSanitizer.sanitize(uuid);
        assertThat(result).isEqualTo(uuid.toString());
    }

    @Test
    void sanitizeStringHandlesEmptyString() {
        String result = LogSanitizer.sanitize("");
        assertThat(result).isEmpty();
    }
}
