package com.novareport.reporter_service.security;

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
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/auth/")
            || path.startsWith("/h2-console")
            || path.equals("/actuator/health")
            || path.startsWith("/swagger-ui")
            || path.equals("/swagger-ui.html")
            || path.equals("/v3/api-docs")
            || path.startsWith("/v3/api-docs/")
            || path.startsWith("/swagger-resources")
            || path.startsWith("/api/v1/internal/")
            || path.equals("/error");
    }

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtService.parse(token).getBody();
                String subject = claims.getSubject();
                String role = claims.get("role", String.class);
                String uid = claims.get("uid", String.class);
                if (subject != null && role != null && uid != null) {
                    var auth = new UsernamePasswordAuthenticationToken(
                        subject,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    request.setAttribute("uid", uid);
                } else {
                    log.debug("JWT missing claims subject={}, role={}, uid={}", subject, role, uid);
                    SecurityContextHolder.clearContext();
                }
            } catch (Exception ex) {
                log.debug("JWT validation failed: {}", ex.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
