import type { RegisterResponse, TokenResponse } from "../types";
import { apiFetch } from "./client";

/* One small function per backend endpoint. Screens call these instead of writing fetch code. */
export const authApi = {
  register: (email: string, password: string) =>
    apiFetch<RegisterResponse>("/register", { method: "POST", body: { email, password }, auth: false }),

  login: (email: string, password: string) =>
    apiFetch<TokenResponse>("/login", { method: "POST", body: { email, password }, auth: false }),

    // Sends the ID token from "Sign in with Google". The backend verifies it and returns OUR tokens.
  googleLogin: (idToken: string) =>
    apiFetch<TokenResponse>("/googleLogin", { method: "POST", body: { idToken }, auth: false }),

  // The backend revokes the refresh token. The access token simply expires on its own.
  logout: (refreshToken: string) =>
    apiFetch<void>("/logout", { method: "POST", body: { refreshToken }, auth: false }),
};
