package com.novareport.notifications_service.util;

/**
 * Utility class for sanitizing log messages to prevent CRLF injection attacks.
 */
public class LogSanitizer {

    private LogSanitizer() {
        // Utility class
    }

    /**
     * Sanitizes a string by removing CRLF and tab characters that could be used for log injection.
     *
     * @param input the input value to sanitize
     * @return sanitized string with CRLF and tab characters replaced
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
