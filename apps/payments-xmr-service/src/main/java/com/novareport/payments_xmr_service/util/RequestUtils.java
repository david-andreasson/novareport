package com.novareport.payments_xmr_service.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
public final class RequestUtils {

    private RequestUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static UUID resolveUserId(HttpServletRequest request) {
        Object uidAttr = request.getAttribute("uid");
        if (uidAttr instanceof String value) {
            try {
                return UUID.fromString(value);
            } catch (IllegalArgumentException e) {
                log.error("Invalid user ID format in request attributes: {}", LogSanitizer.sanitize(request.getRequestURI()));
                throw new IllegalStateException("Invalid user ID");
            }
        }
        throw new IllegalStateException("User ID not found in request");
    }
}
