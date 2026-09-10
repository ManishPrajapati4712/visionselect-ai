package com.visionselect.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Validates the {@code X-Internal-Service-Token} header for all requests
 * under {@code /internal/**}.
 *
 * <p>This endpoint is NOT protected by JWT \u2014 it is exclusively for the
 * Python AI service calling back into Spring Boot. The service token is a
 * shared secret configured via {@code app.security.internal.service-token}
 * environment variable (already in application.yml).
 *
 * <p>NEVER log the token value itself.
 */
@Component
public class InternalServiceTokenFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(InternalServiceTokenFilter.class);
    private static final String TOKEN_HEADER = "X-Internal-Service-Token";
    private static final String INTERNAL_PATH_PREFIX = "/internal/";

    private final String expectedToken;

    public InternalServiceTokenFilter(
            @Value("${app.security.internal.service-token}") String expectedToken) {
        this.expectedToken = expectedToken;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(INTERNAL_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String token = request.getHeader(TOKEN_HEADER);

        if (token == null || token.isBlank()) {
            log.warn("Internal endpoint called without service token: {}", request.getRequestURI());
            writeUnauthorized(response, "Missing X-Internal-Service-Token header");
            return;
        }

        if (!token.equals(expectedToken)) {
            log.warn("Internal endpoint called with invalid service token: {}", request.getRequestURI());
            writeForbidden(response, "Invalid service token");
            return;
        }

        chain.doFilter(request, response);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"success\":false,\"status\":\"error\",\"message\":\"" + message + "\",\"errors\":[]}");
    }

    private void writeForbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(
                "{\"success\":false,\"status\":\"error\",\"message\":\"" + message + "\",\"errors\":[]}");
    }
}
