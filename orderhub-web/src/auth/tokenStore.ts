/*
  WHERE THE TOKENS LIVE

  - access token  -> a plain variable in memory (15 minutes of access). It disappears when the
                     page reloads, and JavaScript on other sites can never reach it.
  - refresh token -> localStorage, so a page reload (or reopening the tab) can silently get a
                     new access token instead of asking the user to sign in again.

  TRADE-OFF: anything in localStorage can be read by malicious JavaScript running on your page
  (an XSS bug). The safer production design is for the backend to send the refresh token as an
  HttpOnly cookie, which JavaScript cannot read. That needs a small backend change, so this
  project keeps the token in localStorage and relies on:
    - short-lived access tokens,
    - refresh-token rotation (each refresh token works exactly once),
    - React escaping everything it renders (do not use dangerouslySetInnerHTML).
*/
const REFRESH_KEY = "orderhub.refreshToken";

let accessToken: string | null = null;

export const tokenStore = {
  getAccessToken: () => accessToken,

  getRefreshToken(): string | null {
    try {
      return localStorage.getItem(REFRESH_KEY);
    } catch {
      return null; // storage can be blocked (private mode, strict settings)
    }
  },

  set(access: string, refresh: string) {
    accessToken = access;
    try {
      localStorage.setItem(REFRESH_KEY, refresh);
    } catch {
      /* ignore: the user simply has to sign in again after a reload */
    }
  },

  clear() {
    accessToken = null;
    try {
      localStorage.removeItem(REFRESH_KEY);
    } catch {
      /* ignore */
    }
  },
};
