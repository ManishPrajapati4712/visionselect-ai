/**
 * AuthContext.tsx
 *
 * Manages JWT authentication state for the application.
 *
 * Token storage strategy (security-first):
 *   - Access token  → in-memory React state only (not persisted anywhere)
 *   - Refresh token → localStorage (necessary for session persistence across reloads;
 *                     only written/read by authService.refreshTokenStorage)
 *
 * On mount: if a refresh token exists in localStorage, silently attempt to
 * restore the session via POST /api/v1/auth/refresh. This lets users stay
 * logged in across page reloads without re-entering credentials.
 *
 * The apiClient's token provider is set here, giving the HTTP client access
 * to the current access token and refresh callback without a circular import.
 */

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { setTokenProvider } from "@/services/apiClient";
import {
  login as apiLogin,
  logout as apiLogout,
  register as apiRegister,
  refreshAccessToken,
  refreshTokenStorage,
} from "@/services/authService";
import type {
  AuthResponse,
  AuthUser,
  LoginRequest,
  RegisterRequest,
} from "@/types";

// ---------------------------------------------------------------------------
// Context shape
// ---------------------------------------------------------------------------

interface AuthState {
  /** The authenticated user, or null if not logged in. */
  user: AuthUser | null;
  /** True while the initial session-restore is in progress. */
  isLoading: boolean;
  /** True when a login/register/logout call is in progress. */
  isAuthenticating: boolean;
  /** Last auth error message, if any. Cleared on next login/register attempt. */
  authError: string | null;
  /** Whether the user is authenticated (user !== null && !isLoading). */
  isAuthenticated: boolean;

  login: (credentials: LoginRequest) => Promise<void>;
  register: (payload: RegisterRequest) => Promise<void>;
  logout: () => Promise<void>;
  clearAuthError: () => void;
}

const AuthCtx = createContext<AuthState | null>(null);

// ---------------------------------------------------------------------------
// Provider
// ---------------------------------------------------------------------------

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isAuthenticating, setIsAuthenticating] = useState(false);
  const [authError, setAuthError] = useState<string | null>(null);

  // Access token is kept in a ref so apiClient can read it synchronously
  // without triggering React re-renders.
  const accessTokenRef = useRef<string | null>(null);

  // Register token provider with apiClient once on mount
  useEffect(() => {
    setTokenProvider({
      getAccessToken: () => accessTokenRef.current,
      getRefreshToken: () => refreshTokenStorage.get(),
      onTokensRefreshed: (newAccess: string, newRefresh: string) => {
        accessTokenRef.current = newAccess;
        refreshTokenStorage.set(newRefresh);
      },
      onSessionExpired: () => {
        accessTokenRef.current = null;
        refreshTokenStorage.clear();
        setUser(null);
      },
    });
  }, []);

  // On mount: try to restore the session from a stored refresh token
  useEffect(() => {
    const storedRefresh = refreshTokenStorage.get();
    if (!storedRefresh) {
      setIsLoading(false);
      return;
    }

    refreshAccessToken(storedRefresh)
      .then((authResponse) => {
        accessTokenRef.current = authResponse.accessToken;
        refreshTokenStorage.set(authResponse.refreshToken);
        setUser(authResponse.user);
      })
      .catch(() => {
        // Stored refresh token is expired or invalid — clear it silently
        refreshTokenStorage.clear();
      })
      .finally(() => {
        setIsLoading(false);
      });
  }, []);

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  const applyAuthResponse = useCallback((response: AuthResponse) => {
    accessTokenRef.current = response.accessToken;
    refreshTokenStorage.set(response.refreshToken);
    setUser(response.user);
  }, []);

  // ---------------------------------------------------------------------------
  // Auth actions
  // ---------------------------------------------------------------------------

  const login = useCallback(
    async (credentials: LoginRequest) => {
      setIsAuthenticating(true);
      setAuthError(null);
      try {
        const response = await apiLogin(credentials);
        applyAuthResponse(response);
      } catch (err: unknown) {
        const msg =
          err instanceof Error ? err.message : "Login failed. Please try again.";
        setAuthError(msg);
        throw err; // let the form component handle it too if needed
      } finally {
        setIsAuthenticating(false);
      }
    },
    [applyAuthResponse],
  );

  const register = useCallback(
    async (payload: RegisterRequest) => {
      setIsAuthenticating(true);
      setAuthError(null);
      try {
        const response = await apiRegister(payload);
        applyAuthResponse(response);
      } catch (err: unknown) {
        const msg =
          err instanceof Error
            ? err.message
            : "Registration failed. Please try again.";
        setAuthError(msg);
        throw err;
      } finally {
        setIsAuthenticating(false);
      }
    },
    [applyAuthResponse],
  );

  const logout = useCallback(async () => {
    setIsAuthenticating(true);
    try {
      // Best-effort server-side revocation — we clear client state regardless
      await apiLogout().catch(() => {});
    } finally {
      accessTokenRef.current = null;
      refreshTokenStorage.clear();
      setUser(null);
      setIsAuthenticating(false);
    }
  }, []);

  const clearAuthError = useCallback(() => setAuthError(null), []);

  // ---------------------------------------------------------------------------
  // Context value
  // ---------------------------------------------------------------------------

  const value = useMemo<AuthState>(
    () => ({
      user,
      isLoading,
      isAuthenticating,
      authError,
      isAuthenticated: user !== null && !isLoading,
      login,
      register,
      logout,
      clearAuthError,
    }),
    [user, isLoading, isAuthenticating, authError, login, register, logout, clearAuthError],
  );

  return <AuthCtx.Provider value={value}>{children}</AuthCtx.Provider>;
}

// ---------------------------------------------------------------------------
// Hook
// ---------------------------------------------------------------------------

export function useAuth(): AuthState {
  const ctx = useContext(AuthCtx);
  if (!ctx) throw new Error("useAuth must be used inside AuthProvider");
  return ctx;
}
