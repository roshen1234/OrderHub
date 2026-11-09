import { beforeEach, describe, expect, it, vi } from "vitest";
import { apiFetch, ApiError } from "./client";
import { tokenStore } from "../auth/tokenStore";

/*
  These tests replace the browser's fetch() with a fake, so no backend is needed.
  They protect the trickiest logic in the app: refreshing an expired access token.
*/
const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json" } });

const newTokens = { accessToken: "new-access", refreshToken: "refresh-2", tokenType: "Bearer", expiresInSeconds: 900 };

/** A fake server: /auth/refresh hands out new tokens, everything else needs the NEW access token. */
function fakeServer(refreshCalls: string[]) {
  return vi.fn(async (url: string, init?: RequestInit) => {
    if (url === "/api/auth/refresh") {
      refreshCalls.push(String(init?.body));
      await new Promise((resolve) => setTimeout(resolve, 20)); // a slow network, so parallel callers overlap
      return json(newTokens);
    }
    const auth = (init?.headers as Record<string, string>)["Authorization"];
    return auth === "Bearer new-access" ? json({ ok: true }) : new Response(null, { status: 401 });
  });
}

describe("apiFetch", () => {
  beforeEach(() => {
    localStorage.clear();
    tokenStore.clear();
  });

  it("refreshes the access token once and repeats the request after a 401", async () => {
    tokenStore.set("old-access", "refresh-1");
    const refreshCalls: string[] = [];
    vi.stubGlobal("fetch", fakeServer(refreshCalls));

    const result = await apiFetch<{ ok: boolean }>("/cart");

    expect(result.ok).toBe(true);
    expect(refreshCalls).toEqual([JSON.stringify({ refreshToken: "refresh-1" })]);
    expect(tokenStore.getRefreshToken()).toBe("refresh-2"); // the rotated token was saved
  });

  it("shares ONE refresh between requests that fail at the same time", async () => {
    tokenStore.set("old-access", "refresh-1");
    const refreshCalls: string[] = [];
    vi.stubGlobal("fetch", fakeServer(refreshCalls));

    const results = await Promise.all([apiFetch("/cart"), apiFetch("/orders"), apiFetch("/addresses")]);

    expect(results).toHaveLength(3);
    expect(refreshCalls).toHaveLength(1); // a refresh token works only once, so it must not be spent three times
  });

  it("turns error bodies into readable messages", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => json({ error: "Cart is empty" }, 400)));
    await expect(apiFetch("/orders", { method: "POST", body: {} })).rejects.toMatchObject({
      status: 400,
      message: "Cart is empty",
    });

    vi.stubGlobal("fetch", vi.fn(async () => json({ email: "must be a well-formed email address" }, 400)));
    await expect(apiFetch("/auth/register", { method: "POST", body: {}, auth: false })).rejects.toThrow(
      "email: must be a well-formed email address",
    );
  });

  it("signs the user out when the refresh token is rejected", async () => {
    tokenStore.set("old-access", "refresh-1");
    vi.stubGlobal("fetch", vi.fn(async () => new Response(null, { status: 401 })));

    await expect(apiFetch("/cart")).rejects.toBeInstanceOf(ApiError);
    expect(tokenStore.getRefreshToken()).toBeNull();
    expect(tokenStore.getAccessToken()).toBeNull();
  });
});
