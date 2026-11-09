import type { Cart, CartItem } from "../types";
import { apiFetch } from "./client";

export const cartApi = {
  get: () => apiFetch<Cart>("/cart"),

  // Adding a product that is already in the cart increases its quantity (the backend merges them).
  addItem: (productId: number, quantity: number) =>
    apiFetch<CartItem>("/cart/items", { method: "POST", body: { productId, quantity } }),

  updateItem: (itemId: number, quantity: number) =>
    apiFetch<CartItem>(`/cart/items/${itemId}`, { method: "PATCH", body: { quantity } }),

  removeItem: (itemId: number) => apiFetch<void>(`/cart/items/${itemId}`, { method: "DELETE" }),
};
