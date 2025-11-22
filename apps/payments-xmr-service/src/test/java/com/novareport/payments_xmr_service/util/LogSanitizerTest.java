package com.novareport.payments_xmr_service.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogSanitizerTest {

    @Test
    void sanitizeReturnsLiteralNullForNullInput() {
        String result = LogSanitizer.sanitize(null);

        assertThat(result).isEqualTo("null");
    }

    @Test
    void sanitizeReplacesControlCharactersWithUnderscore() {
        String input = "line1\nline2\rline3\tend";

        String result = LogSanitizer.sanitize(input);

        assertThat(result).doesNotContain("\n");
        assertThat(result).doesNotContain("\r");
        assertThat(result).doesNotContain("\t");
        assertThat(result).isEqualTo("line1_line2_line3_end");
    }

    @Test
    void sanitizePreservesNormalText() {
        String input = "normal-text-123";

        String result = LogSanitizer.sanitize(input);

        assertThat(result).isEqualTo("normal-text-123");
    }
}
