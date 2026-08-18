/**
 * authService.ts
 *
 * All auth-related API calls to the Spring Boot backend.
 * Endpoints used:
 *   POST /api/v1/auth/register  → public
 *   POST /api/v1/auth/login     → public
 *   POST /api/v1/auth/refresh   → public (handled internally by apiClient on 401)
 *   POST /api/v1/auth/logout    → requires JWT
 *   GET  /api/v1/users/me       → requires JWT
 *
 * SECURITY: No JWT secret or DB credential ever passes through this file.
 * Access token is kept in memory (never localStorage).
 * Refresh token is stored in localStorage only here, for persistence across reloads.
 */

import { apiClient } from "@/services/apiClient";
import type {
  AuthResponse,
  AuthUser,
  LoginRequest,
  RegisterRequest,
} from "@/types";

// ---------------------------------------------------------------------------
// Storage key — refresh token only, not access token
// ---------------------------------------------------------------------------
const REFRESH_TOKEN_KEY = "vs_refresh_token";

export const refreshTokenStorage = {
  get: (): string | null => localStorage.getItem(REFRESH_TOKEN_KEY),
  set: (token: string) => localStorage.setItem(REFRESH_TOKEN_KEY, token),
  clear: () => localStorage.removeItem(REFRESH_TOKEN_KEY),
};

// ---------------------------------------------------------------------------
// Auth API calls
// ---------------------------------------------------------------------------

/**
 * POST /api/v1/auth/login
 * Returns { accessToken, refreshToken, user }.
 * auth: false — no bearer token needed.
 */
export async function login(credentials: LoginRequest): Promise<AuthResponse> {
  return apiClient.post<AuthResponse>("/api/v1/auth/login", credentials, false);
}

/**
 * POST /api/v1/auth/register
 * Returns { accessToken, refreshToken, user }.
 * auth: false — no bearer token needed.
 */
export async function register(payload: RegisterRequest): Promise<AuthResponse> {
  return apiClient.post<AuthResponse>("/api/v1/auth/register", payload, false);
}

/**
 * POST /api/v1/auth/refresh
 * Returns new { accessToken, refreshToken }.
 * Called automatically by apiClient on 401, but also exported for
 * explicit use (e.g. on app load to restore a session).
 * auth: false — uses the refresh token, not the access token.
 */
export async function refreshAccessToken(refreshToken: string): Promise<AuthResponse> {
  return apiClient.post<AuthResponse>(
    "/api/v1/auth/refresh",
    { refreshToken },
    false,
  );
}

/**
 * POST /api/v1/auth/logout
 * Requires a valid access token. The backend revokes the refresh token server-side.
 * auth: true (default).
 */
export async function logout(): Promise<void> {
  await apiClient.post<void>("/api/v1/auth/logout", {}, true);
}

/**
 * GET /api/v1/users/me
 * Returns the currently authenticated user's profile.
 * auth: true (default).
 */
export async function getMe(): Promise<AuthUser> {
  return apiClient.get<AuthUser>("/api/v1/users/me", true);
}
