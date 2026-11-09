import type { TokenResponse } from "../types";
import { tokenStore } from "../auth/tokenStore";

/*
  THE API CLIENT
  Every request to the backend goes through apiFetch(). It does four jobs:
    1. builds the URL (everything starts with /api, which the Vite dev proxy forwards to Spring),
    2. adds "Authorization: Bearer <access token>" when we have one,
    3. turns error responses into a readable ApiError,
    4. when the server answers 401 (access token expired), gets a new access token with the
       refresh token, then repeats the request once. The user never notices.
*/
const BASE = "/api";

/** An error that carries the HTTP status, so screens can react to 409, 404 and so on. */
export class ApiError extends Error {
  status: number;
  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

/**
 * The backend reports errors in two shapes:
 *   { "error": "Cart is empty" }                          (business errors)
 *   { "email": "must be a well-formed email address" }    (validation errors, one entry per field)
 * This turns either one into a single sentence for the screen.
 */
function messageFromBody(body: unknown, fallback: string): string {
  if (body && typeof body === "object") {
    const record = body as Record<string, unknown>;
    if (typeof record.error === "string") return record.error;
    const parts = Object.entries(record)
      .filter(([, v]) => typeof v === "string")
      .map(([field, message]) => `${field}: ${message}`);
    if (parts.length > 0) return parts.join(". ");
  }
  return fallback;
}

// ---------------------------------------------------------------------------
// Refreshing the session
// ---------------------------------------------------------------------------

/** Set by AuthProvider: called when the refresh token is rejected and the user must sign in again. */
let onSessionExpired: (() => void) | null = null;
export const setSessionExpiredHandler = (handler: (() => void) | null) => {
  onSessionExpired = handler;
};

/*
  The backend ROTATES refresh tokens: each one works exactly once, and using an old one makes the
  server revoke the whole session. So two refresh calls must never run at the same time with the
  same token. Two guards:
    - inFlight: inside this tab, concurrent callers share one network call.
    - navigator.locks: across browser tabs, calls take turns (and re-read the latest token).
*/
let inFlight: Promise<boolean> | null = null;

function withTabLock<T>(task: () => Promise<T>): Promise<T> {
  if (typeof navigator !== "undefined" && "locks" in navigator) {
    return navigator.locks.request("orderhub-token-refresh", task);
  }
  return task();
}

/** Trades the stored refresh token for a new token pair. Returns true if we are signed in afterwards. */
export function refreshSession(): Promise<boolean> {
  if (inFlight) return inFlight;

  inFlight = withTabLock(async () => {
    const refreshToken = tokenStore.getRefreshToken(); // read inside the lock: another tab may have rotated it
    if (!refreshToken) return false;
    try {
      const res = await fetch(`${BASE}/refresh`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ refreshToken }),
      });
      if (!res.ok) {
        // 401: expired, revoked or reused. The session is over.
        tokenStore.clear();
        onSessionExpired?.();
        return false;
      }
      const tokens = (await res.json()) as TokenResponse;
      tokenStore.set(tokens.accessToken, tokens.refreshToken); // always save the NEW refresh token
      return true;
    } catch {
      return false; // network error: keep the tokens, we may be offline for a moment
    }
  }).finally(() => {
    inFlight = null;
  });

  return inFlight;
}

// ---------------------------------------------------------------------------
// The request function
// ---------------------------------------------------------------------------

interface RequestOptions {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  body?: unknown; // objects are sent as JSON
  headers?: Record<string, string>;
  /** false for login/register, which have no token to send. */
  auth?: boolean;
}

export async function apiFetch<T>(path: string, options: RequestOptions = {}, isRetry = false): Promise<T> {
  const { method = "GET", body, headers = {}, auth = true } = options;

  const requestHeaders: Record<string, string> = { ...headers };
  if (body !== undefined) requestHeaders["Content-Type"] = "application/json";
  const accessToken = tokenStore.getAccessToken();
  if (auth && accessToken) requestHeaders["Authorization"] = `Bearer ${accessToken}`;

  const res = await fetch(`${BASE}${path}`, {
    method,
    headers: requestHeaders,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  // Access token expired (or missing after a reload): refresh once, then repeat the request.
  if (res.status === 401 && auth && !isRetry && tokenStore.getRefreshToken()) {
    if (await refreshSession()) return apiFetch<T>(path, options, true);
  }

  if (!res.ok) {
    let parsed: unknown = null;
    try {
      parsed = await res.json();
    } catch {
      /* some errors (like a plain 401) have no body */
    }
    const fallback =
      res.status === 401 ? "Please sign in to continue." :
      res.status === 403 ? "You do not have permission to do that." :
      res.status >= 500 ? "The server had a problem. Please try again." :
      `Request failed (${res.status}).`;
    throw new ApiError(res.status, messageFromBody(parsed, fallback));
  }

  // Success. A 204 has no body (cancel, delete, ship and so on).
  if (res.status === 204) return undefined as T;
  const contentType = res.headers.get("content-type") ?? "";
  if (contentType.includes("application/json")) return (await res.json()) as T;
  return (await res.text()) as T;
}
