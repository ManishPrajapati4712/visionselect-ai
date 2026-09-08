package com.visionselect.backend.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.visionselect.backend.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Returns an {@link ApiResponse} 401 instead of Spring Security's default
 * redirect/plain-text response when a protected endpoint is accessed without
 * a valid access token.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiResponse<Void> body = ApiResponse.error(
                "Authentication required",
                List.of(Map.of("code", "UNAUTHORIZED", "message", "Authentication required"))
        );
        objectMapper.writeValue(response.getWriter(), body);
    }
}
