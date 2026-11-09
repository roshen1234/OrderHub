import { beforeEach, describe, expect, it, vi } from "vitest";
import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { tokenStore } from "./auth/tokenStore";
import { fakeJwt, renderApp } from "./test/renderApp";

const json = (body: unknown, status = 200) =>
  new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json" } });

const page = {
  content: [
    { id: 1, name: "iPhone 15", description: "Phone", price: 799, categoryId: 1, categoryName: "Phones", active: true },
    { id: 2, name: "Clean Code", description: "Book", price: 34.5, categoryId: 2, categoryName: "Books", active: true },
  ],
  page: 0, size: 12, totalElements: 2, totalPages: 1,
};

/** A fake backend with just enough routes for these tests. */
function stubBackend(roles: string[]) {
  vi.stubGlobal("fetch", vi.fn(async (url: string) => {
    if (url.startsWith("/api/products")) return json(page);
    if (url === "/api/categories") return json([{ id: 1, name: "Phones", parentId: null }]);
    if (url === "/api/cart") return json({ cartId: 1, items: [{ id: 1, productId: 1, productName: "iPhone 15", unitPrice: 799, quantity: 2, lineTotal: 1598 }], total: 1598 });
    if (url === "/api/auth/login") {
      const accessToken = fakeJwt({ sub: "ana@example.com", roles, exp: 9999999999 });
      return json({ accessToken, refreshToken: "refresh-1", tokenType: "Bearer", expiresInSeconds: 900 });
    }
    return new Response(null, { status: 404 });
  }));
}

describe("the app", () => {
  beforeEach(() => {
    localStorage.clear();
    tokenStore.clear();
  });

  it("lists products for a visitor who is not signed in", async () => {
    stubBackend([]);
    renderApp("/products");
    expect(await screen.findByText("iPhone 15")).toBeTruthy();
    expect(screen.getByText("Clean Code")).toBeTruthy();
    expect(screen.getByText("Sign in")).toBeTruthy();
  });

  it("sends a visitor to the login page when they open a protected page", async () => {
    stubBackend([]);
    renderApp("/cart");
    expect(await screen.findByRole("heading", { name: "Sign in" })).toBeTruthy();
  });

  it("signs a customer in, shows the cart count, and hides admin links", async () => {
    stubBackend(["ROLE_CUSTOMER"]);
    renderApp("/login");
    await userEvent.type(await screen.findByLabelText("Email"), "ana@example.com");
    await userEvent.type(screen.getByLabelText("Password"), "Passw0rd!123");
    await userEvent.click(screen.getByRole("button", { name: "Sign in" }));

    expect(await screen.findByText("ana@example.com")).toBeTruthy();
    await waitFor(() => expect(screen.getByText("2")).toBeTruthy()); // 2 units in the cart badge
    expect(screen.queryByText("Admin")).toBeNull();
    expect(tokenStore.getRefreshToken()).toBe("refresh-1");
  });

  it("shows the admin link to an admin", async () => {
    stubBackend(["ROLE_ADMIN"]);
    renderApp("/login");
    await userEvent.type(await screen.findByLabelText("Email"), "ana@example.com");
    await userEvent.type(screen.getByLabelText("Password"), "Passw0rd!123");
    await userEvent.click(screen.getByRole("button", { name: "Sign in" }));
    expect(await screen.findByText("Admin")).toBeTruthy();
  });
});
