package com.novareport.accounts_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class InternalApiKeyFilter extends OncePerRequestFilter {

    private final String expectedKey;

    public InternalApiKeyFilter(String expectedKey) {
        this.expectedKey = expectedKey;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/accounts/internal/");
    }

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        if (!StringUtils.hasText(expectedKey)) {
            response.sendError(HttpStatus.FORBIDDEN.value(), "Internal API key not configured");
            return;
        }

        String provided = request.getHeader("X-INTERNAL-KEY");
        if (!expectedKey.equals(provided)) {
            response.sendError(HttpStatus.FORBIDDEN.value(), "Invalid internal API key");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
