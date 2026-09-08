package com.visionselect.backend.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.visionselect.backend.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Returns an {@link ApiResponse} 403 when an authenticated user lacks the
 * required role for a {@code @PreAuthorize}-protected endpoint.
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiResponse<Void> body = ApiResponse.error(
                "Access denied: you do not have permission to perform this action",
                List.of(Map.of("code", "FORBIDDEN", "message", "Insufficient role"))
        );
        objectMapper.writeValue(response.getWriter(), body);
    }
}
