package com.visionselect.backend.auth.controller;

import com.visionselect.backend.auth.dto.AuthResponse;
import com.visionselect.backend.auth.dto.LoginRequest;
import com.visionselect.backend.auth.dto.RefreshRequest;
import com.visionselect.backend.auth.dto.RegisterRequest;
import com.visionselect.backend.auth.security.AuthenticatedUser;
import com.visionselect.backend.auth.service.AuthService;
import com.visionselect.backend.common.response.ApiResponse;
import com.visionselect.backend.common.util.ApiPaths;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Auth endpoints per security-contract.md and the frontend's authService.ts:
 *
 * <ul>
 *   <li>{@code POST /api/v1/auth/register} — public</li>
 *   <li>{@code POST /api/v1/auth/login}    — public</li>
 *   <li>{@code POST /api/v1/auth/refresh}  — public (refresh token is the credential)</li>
 *   <li>{@code POST /api/v1/auth/logout}   — requires JWT (revokes server-side token)</li>
 * </ul>
 *
 * <p>All responses use the standard {@link ApiResponse} envelope.
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.success(authService.refresh(request));
    }

    /**
     * Logout requires a valid JWT (the SecurityConfig does not permit this
     * path anonymously). Revokes all active refresh tokens for the caller.
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal AuthenticatedUser principal) {
        authService.logout(principal);
        return ApiResponse.success(null, "Logged out successfully");
    }
}
