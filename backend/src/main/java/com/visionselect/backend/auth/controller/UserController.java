package com.visionselect.backend.auth.controller;

import com.visionselect.backend.auth.dto.AuthUserDto;
import com.visionselect.backend.auth.security.AuthenticatedUser;
import com.visionselect.backend.auth.service.AuthService;
import com.visionselect.backend.common.response.ApiResponse;
import com.visionselect.backend.common.util.ApiPaths;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * User self-service endpoint.
 *
 * <p>{@code GET /api/v1/users/me} — returns the authenticated user's profile.
 * Called by the frontend's {@code getMe()} in {@code authService.ts} and used
 * by {@code AuthContext} to restore session state.
 *
 * <p>Authentication is required (falls through to {@code .anyRequest().authenticated()}
 * in SecurityConfig). Role-level narrowing is not needed: this endpoint always
 * returns the caller's own record (no id parameter).
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/users")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    public ApiResponse<AuthUserDto> getMe(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ApiResponse.success(authService.getMe(principal));
    }
}
