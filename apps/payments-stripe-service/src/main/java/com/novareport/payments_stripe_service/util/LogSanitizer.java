package com.novareport.payments_stripe_service.util;

/**
 * Utility class for sanitizing log messages to prevent CRLF injection attacks.
 */
public final class LogSanitizer {

    private LogSanitizer() {
        // Utility class
    }

    /**
     * Sanitizes a string by removing CRLF characters that could be used for log injection.
     *
     * @param input the input object to sanitize
     * @return sanitized string with CRLF characters removed
     */
    public static String sanitize(Object input) {
        if (input == null) {
            return "null";
        }
        return input.toString()
                .replace('\n', '_')
                .replace('\r', '_')
                .replace('\t', '_');
    }
}
