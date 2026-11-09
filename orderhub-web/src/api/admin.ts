import type { Category, Order, OrderSummary, PageResponse, Product, ProductInput, StockResponse } from "../types";
import { apiFetch } from "./client";
import { toQueryString } from "./catalog";

/* Endpoints that need the ADMIN role. A customer's token gets 403 here. */
export const adminApi = {
  // ----- catalog -----
  createCategory: (name: string, parentId: number | null) =>
    apiFetch<Category>("/categories", { method: "POST", body: { name, parentId } }),

  createProduct: (input: ProductInput & { initialStock: number }) =>
    apiFetch<Product>("/products", { method: "POST", body: input }),

  updateProduct: (id: number, input: ProductInput) =>
    apiFetch<Product>(`/products/${id}`, { method: "PUT", body: input }),

  // "Delete" only deactivates the product, so old orders keep their history.
  deactivateProduct: (id: number) => apiFetch<void>(`/products/${id}`, { method: "DELETE" }),

  // delta is relative: +20 restocks, -5 removes stock. The backend refuses to go below zero (409).
  adjustStock: (productId: number, delta: number) =>
    apiFetch<StockResponse>(`/inventory/${productId}`, { method: "PATCH", body: { delta } }),

  // Current stock for several products at once: GET /inventory?productIds=1,2,3 (admin only).
  // One request for the whole table page, instead of one request per row.
  getStocks: (productIds: number[]) =>
    apiFetch<StockResponse[]>(`/inventory${toQueryString({ productIds: productIds.join(",") })}`),

  // ----- orders -----
  listOrders: (params: { status?: string; page: number; size?: number }) =>
    apiFetch<PageResponse<OrderSummary>>(`/admin/orders${toQueryString({ size: 15, ...params })}`),

  getOrder: (id: number) => apiFetch<Order>(`/admin/orders/${id}`),
  shipOrder: (id: number) => apiFetch<void>(`/admin/orders/${id}/ship`, { method: "POST" }),
  deliverOrder: (id: number) => apiFetch<void>(`/admin/orders/${id}/deliver`, { method: "POST" }),

  // ----- development helper (only exists while the backend uses the mock payment gateway) -----
  simulateGateway: (body: { providerRef: string; success: boolean; eventId?: string; sendWebhook: boolean }) =>
    apiFetch<string>("/dev/gateway/simulate", { method: "POST", body }),
};
