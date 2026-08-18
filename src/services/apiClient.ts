/**
 * apiClient.ts
 *
 * Central HTTP client for all backend API calls.
 * Reads VITE_API_BASE_URL — the only env var the frontend has for backend connectivity.
 *
 * Responsibilities:
 *  - Prefix every request with the backend base URL
 *  - Attach the JWT access token as a Bearer header on protected requests
 *  - Unwrap the ApiResponse<T> envelope and throw on success: false
 *  - On 401, attempt one silent token refresh then retry the original request
 *  - Expose a setTokenProvider() so AuthContext can inject token accessors
 *    without creating a circular import
 *
 * SECURITY RULES (enforced here):
 *  - Access token is kept in memory (supplied by AuthContext), never in localStorage
 *  - Refresh token lives in localStorage only; it is never sent to any endpoint
 *    other than POST /auth/refresh
 *  - No backend secrets cross this file
 */

import type { ApiResponse } from "@/types";

// ---------------------------------------------------------------------------
// Configuration
// ---------------------------------------------------------------------------

const API_BASE = import.meta.env['VITE_API_BASE_URL'] as string;

if (!API_BASE) {
  console.warn(
    "[apiClient] VITE_API_BASE_URL is not set. " +
      "Create a .env.local file with VITE_API_BASE_URL=http://localhost:8080",
  );
}

// ---------------------------------------------------------------------------
// Token provider — injected by AuthContext to avoid circular imports
// ---------------------------------------------------------------------------

interface TokenProvider {
  getAccessToken: () => string | null;
  getRefreshToken: () => string | null;
  onTokensRefreshed: (accessToken: string, refreshToken: string) => void;
  onSessionExpired: () => void;
}

let tokenProvider: TokenProvider | null = null;

/**
 * Called once by AuthContext on mount. Provides the client with accessor
 * callbacks so it can read/update tokens without importing AuthContext.
 */
export function setTokenProvider(provider: TokenProvider): void {
  tokenProvider = provider;
}

// ---------------------------------------------------------------------------
// ApiClientError — structured error with backend details
// ---------------------------------------------------------------------------

export class ApiClientError extends Error {
  constructor(
    public readonly status: number,
    message: string,
    public readonly errors: string[] | null = null,
  ) {
    super(message);
    this.name = "ApiClientError";
  }
}

// ---------------------------------------------------------------------------
// Core fetch wrapper
// ---------------------------------------------------------------------------

/** Whether a silent token-refresh is currently in flight (prevents concurrent refreshes). */
let isRefreshing = false;
/** Callbacks waiting for the in-flight refresh to complete. */
let refreshSubscribers: Array<(token: string) => void> = [];

function subscribeToRefresh(cb: (token: string) => void) {
  refreshSubscribers.push(cb);
}

function notifyRefreshSubscribers(token: string) {
  refreshSubscribers.forEach((cb) => cb(token));
  refreshSubscribers = [];
}

async function doRefresh(): Promise<string | null> {
  const refreshToken = tokenProvider?.getRefreshToken();
  if (!refreshToken) return null;

  try {
    const res = await fetch(`${API_BASE}/api/v1/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    });

    if (!res.ok) return null;

    const envelope: ApiResponse<{ accessToken: string; refreshToken: string }> =
      await res.json();
    if (!envelope.success || !envelope.data) return null;

    const { accessToken, refreshToken: newRefresh } = envelope.data;
    tokenProvider?.onTokensRefreshed(accessToken, newRefresh);
    return accessToken;
  } catch {
    return null;
  }
}

/**
 * Core request function. Pass `auth: true` (default) to include the
 * Authorization header. `auth: false` is used for public endpoints
 * (login, register, refresh).
 */
async function request<T>(
  path: string,
  options: RequestInit & { auth?: boolean } = {},
): Promise<T> {
  const { auth = true, headers: extraHeaders, ...rest } = options;

  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...(extraHeaders as Record<string, string>),
  };

  if (auth && tokenProvider) {
    const token = tokenProvider.getAccessToken();
    if (token) {
      headers["Authorization"] = `Bearer ${token}`;
    }
  }

  const res = await fetch(`${API_BASE}${path}`, { ...rest, headers });

  // --- 401: attempt silent token refresh once ---
  if (res.status === 401 && auth && tokenProvider) {
    if (!isRefreshing) {
      isRefreshing = true;
      const newToken = await doRefresh();
      isRefreshing = false;

      if (newToken) {
        notifyRefreshSubscribers(newToken);
        // Retry the original request with the new token
        const retryHeaders = {
          ...headers,
          Authorization: `Bearer ${newToken}`,
        };
        const retryRes = await fetch(`${API_BASE}${path}`, {
          ...rest,
          headers: retryHeaders,
        });
        return parseResponse<T>(retryRes);
      } else {
        tokenProvider.onSessionExpired();
        throw new ApiClientError(401, "Session expired. Please log in again.");
      }
    } else {
      // Another refresh is already in flight — queue behind it
      return new Promise<T>((resolve, reject) => {
        subscribeToRefresh((newToken) => {
          const retryHeaders = {
            ...headers,
            Authorization: `Bearer ${newToken}`,
          };
          fetch(`${API_BASE}${path}`, { ...rest, headers: retryHeaders })
            .then((r) => parseResponse<T>(r))
            .then(resolve)
            .catch(reject);
        });
      });
    }
  }

  return parseResponse<T>(res);
}

async function parseResponse<T>(res: Response): Promise<T> {
  let envelope: ApiResponse<T>;
  try {
    envelope = await res.json();
  } catch {
    throw new ApiClientError(res.status, `HTTP ${res.status}: ${res.statusText}`);
  }

  if (!envelope.success || !res.ok) {
    const msg: string =
      envelope.message ??
      (envelope.errors && envelope.errors.length > 0
        ? envelope.errors[0]
        : `HTTP ${res.status}`) ??
      `HTTP ${res.status}`;
    throw new ApiClientError(res.status, msg, envelope.errors);
  }

  return envelope.data as T;
}

// ---------------------------------------------------------------------------
// Public API — typed convenience methods
// ---------------------------------------------------------------------------

export const apiClient = {
  get<T>(path: string, auth = true): Promise<T> {
    return request<T>(path, { method: "GET", auth });
  },

  post<T>(path: string, body: unknown, auth = true): Promise<T> {
    return request<T>(path, {
      method: "POST",
      body: JSON.stringify(body),
      auth,
    });
  },

  put<T>(path: string, body: unknown, auth = true): Promise<T> {
    return request<T>(path, {
      method: "PUT",
      body: JSON.stringify(body),
      auth,
    });
  },

  delete<T>(path: string, auth = true): Promise<T> {
    return request<T>(path, { method: "DELETE", auth });
  },

  /**
   * Upload raw bytes to a pre-signed URL (the /local-storage/upload endpoint in dev,
   * or a real S3/GCS signed URL in production).
   * No Authorization header is sent — the signed URL itself is the credential.
   * Returns true on 2xx.
   */
  async putBytes(
    url: string,
    file: File,
    onProgress?: (pct: number) => void,
  ): Promise<void> {
    return new Promise<void>((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      xhr.open("PUT", url, true);
      xhr.setRequestHeader("Content-Type", file.type || "application/octet-stream");

      if (onProgress) {
        xhr.upload.addEventListener("progress", (e) => {
          if (e.lengthComputable) {
            onProgress(Math.round((e.loaded / e.total) * 100));
          }
        });
      }

      xhr.onload = () => {
        if (xhr.status >= 200 && xhr.status < 300) {
          resolve();
        } else {
          reject(new ApiClientError(xhr.status, `Upload failed: HTTP ${xhr.status}`));
        }
      };

      xhr.onerror = () =>
        reject(new ApiClientError(0, "Network error during file upload."));

      xhr.send(file);
    });
  },
};
