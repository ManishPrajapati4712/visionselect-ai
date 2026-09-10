package com.visionselect.backend.config;

import com.visionselect.backend.auth.security.JwtAuthenticationEntryPoint;
import com.visionselect.backend.auth.security.JwtAuthenticationFilter;
import com.visionselect.backend.auth.security.RestAccessDeniedHandler;
import com.visionselect.backend.common.util.ApiPaths;
import com.visionselect.backend.config.InternalServiceTokenFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Security configuration for the auth module (Phase 2C).
 *
 * <p>What's real as of this phase:
 * <ul>
 *   <li>Stateless sessions, CSRF disabled, explicit CORS allowlist -
 *       unchanged from the Phase 2B foundation.</li>
 *   <li>{@link JwtAuthenticationFilter} runs before
 *       {@code UsernamePasswordAuthenticationFilter} and populates the
 *       SecurityContext from a valid bearer access token.</li>
 *   <li>{@code /api/v1/auth/register}, {@code /login}, {@code /refresh}
 *       are public (security: [] in openapi.yaml - credentials aren't
 *       required for these). Everything else defaults to
 *       {@code authenticated()}, including {@code /auth/logout} and
 *       {@code /users/me}.</li>
 *   <li>{@link JwtAuthenticationEntryPoint} / {@link RestAccessDeniedHandler}
 *       produce the standard ApiResponse envelope for 401 / 403
 *       respectively, instead of Spring Security's default responses.</li>
 *   <li>{@code @EnableMethodSecurity} is turned on so future business
 *       modules can enforce the rest of security-contract.md's RBAC table
 *       with {@code @PreAuthorize("hasRole('COACH')")} etc. Nothing uses
 *       it yet - Auth is "all roles" and {@code /users/me} is "own record
 *       only" by construction (no id parameter, always the caller's own
 *       account) - security-contract.md's RBAC table for both groups is
 *       already satisfied without an explicit role check.</li>
 * </ul>
 *
 * <p>{@code /internal/**} (the AI callback) is deliberately not addressed
 * here - that endpoint doesn't exist yet (out of scope for Phase 2C) and
 * will use a different auth scheme (service token, not JWT) when it does.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;
    private final InternalServiceTokenFilter internalServiceTokenFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                           JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                           RestAccessDeniedHandler restAccessDeniedHandler,
                           InternalServiceTokenFilter internalServiceTokenFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.restAccessDeniedHandler = restAccessDeniedHandler;
        this.internalServiceTokenFilter = internalServiceTokenFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler)
                )
                .authorizeHttpRequests(authorize -> authorize
                        // Public per openapi.yaml (each declares `security: []`).
                        .requestMatchers(
                                ApiPaths.V1 + "/auth/register",
                                ApiPaths.V1 + "/auth/login",
                                ApiPaths.V1 + "/auth/refresh"
                        ).permitAll()
                        // Ops / docs - not business endpoints, safe to expose.
                        .requestMatchers(
                                "/actuator/health", "/actuator/info",
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"
                        ).permitAll()
                        // Dev/demo local-storage shim (LocalStorageProvider): its own
                        // per-object signed token is the credential here, standing in
                        // for a real cloud provider's URL signature - a browser PUTting
                        // straight to S3 doesn't carry this backend's JWT either, and
                        // this endpoint is not part of the versioned /api/v1 contract.
                        .requestMatchers("/local-storage/**").permitAll()
                        // Internal AI callback: protected by X-Internal-Service-Token
                        // header (InternalServiceTokenFilter), NOT by JWT. The filter
                        // runs before this chain and rejects invalid tokens with 401/403.
                        .requestMatchers("/internal/**").permitAll()
                        // Everything else (including /auth/logout, /users/me,
                        // and every business-module route added in later
                        // phases) requires a valid access token by default.
                        // Role-specific narrowing (the rest of
                        // security-contract.md's RBAC table) is applied at
                        // the controller/method level as those modules are
                        // built, via @PreAuthorize.
                        .anyRequest().authenticated()
                )
                // InternalServiceTokenFilter runs before the JWT filter so that
                // /internal/** requests are validated by service-token and never
                // reach the JWT authentication machinery.
                .addFilterBefore(internalServiceTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Internal-Service-Token", "Idempotency-Key"));
        configuration.setExposedHeaders(List.of("X-Request-Id"));
        configuration.setAllowCredentials(false); // bearer-token auth, not cookies
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /** Password hashing algorithm per security-contract.md ("bcrypt or argon2"). Used by AuthService. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
