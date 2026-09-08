package com.visionselect.backend.auth.security;

import com.visionselect.backend.auth.entity.UserRole;
import com.visionselect.backend.auth.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Reads the {@code Authorization: Bearer <token>} header on each request,
 * validates the JWT, and populates the SecurityContext with an
 * {@link AuthenticatedUser} principal if valid.
 *
 * <p>Runs before {@code UsernamePasswordAuthenticationFilter} per the
 * {@code SecurityConfig} filter order declaration.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length());
        jwtService.validateAndExtract(token).ifPresent(principal -> {
            // Build Spring authority string: ROLE_ADMIN, ROLE_COACH, etc.
            // This is required for @PreAuthorize("hasAnyRole('ADMIN','COACH')") to work.
            String authorityName = "ROLE_" + principal.role().name();
            List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(authorityName));

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        });

        filterChain.doFilter(request, response);
    }
}
