package com.novareport.accounts_service.filter;

import com.novareport.accounts_service.config.RateLimitingConfig;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> rateLimitBuckets;
    private final RateLimitingConfig rateLimitingConfig;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        
        String path = request.getRequestURI();
        if (isAuthRateLimitedPath(path)) {
            String clientIp = getClientIP(request);
            Bucket bucket = rateLimitBuckets.computeIfAbsent(clientIp, k -> rateLimitingConfig.createBucket());

            if (bucket.tryConsume(1)) {
                filterChain.doFilter(request, response);
            } else {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write("För många inloggningsförsök. Vänta en stund och försök igen.");
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private boolean isAuthRateLimitedPath(String path) {
        return path.startsWith("/auth/login")
                || path.startsWith("/auth/register")
                || path.startsWith("/api/accounts/auth/login")
                || path.startsWith("/api/accounts/auth/register");
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
