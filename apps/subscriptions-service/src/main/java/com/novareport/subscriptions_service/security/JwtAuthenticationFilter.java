package com.novareport.subscriptions_service.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        boolean skip = path.startsWith("/auth/") ||
            path.startsWith("/h2-console") ||
            path.equals("/actuator/health") ||
            path.startsWith("/swagger-ui") ||
            path.equals("/swagger-ui.html") ||
            path.equals("/v3/api-docs") ||
            path.startsWith("/v3/api-docs/") ||
            path.startsWith("/swagger-resources") ||
            path.startsWith("/api/v1/internal/") ||
            path.equals("/error");
        log.debug("shouldNotFilter for path '{}': {}", path, skip);
        return skip;
    }

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain chain
    ) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtService.parse(token);
                String subject = claims.getSubject();
                String role = (String) claims.get("role");
                Object uidClaim = claims.get("uid");
                if (subject != null && role != null && uidClaim instanceof String uidValue) {
                    var auth = new UsernamePasswordAuthenticationToken(
                        subject,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    request.setAttribute("uid", uidValue);
                } else {
                    log.debug("JWT missing required claims for subject={}, role={}, uid={}", subject, role, uidClaim);
                    SecurityContextHolder.clearContext();
                }
            } catch (Exception ex) {
                log.debug("JWT authentication failed: {}", ex.getMessage(), ex);
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
