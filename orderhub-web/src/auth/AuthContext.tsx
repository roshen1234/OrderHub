import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { authApi } from "../api/auth";
import { refreshSession, setSessionExpiredHandler } from "../api/client";
import { decodeJwt } from "../lib/jwt";
import { tokenStore } from "./tokenStore";

/*
  AUTH CONTEXT
  React Context lets any component read "who is signed in" without passing props down through
  every level. Components call useAuth() to get the current user and the login/logout functions.
*/
export interface AuthUser {
  email: string;
  roles: string[];
}

interface AuthContextValue {
  user: AuthUser | null;
  isAdmin: boolean;
  /** true while we are checking, at startup, whether a saved session can be restored */
  initializing: boolean;
  login: (email: string, password: string) => Promise<void>;
  register: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

/** Builds the user object from the access token currently held in memory. */
function userFromToken(): AuthUser | null {
  const token = tokenStore.getAccessToken();
  const payload = token ? decodeJwt(token) : null;
  return payload ? { email: payload.sub, roles: payload.roles ?? [] } : null;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient();
  const [user, setUser] = useState<AuthUser | null>(null);
  const [initializing, setInitializing] = useState(true);

  // When the API client finds the session is over (refresh token rejected), sign out locally.
  useEffect(() => {
    setSessionExpiredHandler(() => {
      setUser(null);
      queryClient.clear(); // forget every cached response that belonged to that user
    });
    return () => setSessionExpiredHandler(null);
  }, [queryClient]);

  // On first load the access token (memory only) is gone, but the refresh token may still be saved.
  // Use it to restore the session. React StrictMode runs effects twice in development; refreshSession()
  // shares one in-flight request, so the single-use refresh token is not spent twice.
  useEffect(() => {
    let cancelled = false;
    (async () => {
      if (tokenStore.getRefreshToken() && (await refreshSession())) {
        if (!cancelled) setUser(userFromToken());
      }
      if (!cancelled) setInitializing(false);
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const tokens = await authApi.login(email, password);
    tokenStore.set(tokens.accessToken, tokens.refreshToken);
    setUser(userFromToken());
  }, []);

  const register = useCallback(
    async (email: string, password: string) => {
      await authApi.register(email, password);
      await login(email, password); // registering does not sign you in, so do it right away
    },
    [login],
  );

  const logout = useCallback(async () => {
    const refreshToken = tokenStore.getRefreshToken();
    try {
      if (refreshToken) await authApi.logout(refreshToken); // tell the server to revoke it
    } catch {
      /* even if the call fails we still sign out locally */
    }
    tokenStore.clear();
    setUser(null);
    queryClient.clear();
  }, [queryClient]);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isAdmin: user?.roles.includes("ROLE_ADMIN") ?? false,
      initializing,
      login,
      register,
      logout,
    }),
    [user, initializing, login, register, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

/** The hook every component uses to read the auth state. */
// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside <AuthProvider>");
  return ctx;
}
