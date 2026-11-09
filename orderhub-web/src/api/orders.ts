import type { Order, OrderSummary, PageResponse, Payment } from "../types";
import { apiFetch } from "./client";
import { toQueryString } from "./catalog";

export const orderApi = {
  // Checkout: the backend builds the order from the signed-in user's cart and the chosen address.
  checkout: (addressId: number) => apiFetch<Order>("/orders", { method: "POST", body: { addressId } }),

  list: (page: number, size = 10) =>
    apiFetch<PageResponse<OrderSummary>>(`/orders${toQueryString({ page, size })}`),

  get: (id: number) => apiFetch<Order>(`/orders/${id}`),

  cancel: (id: number) => apiFetch<void>(`/orders/${id}/cancel`, { method: "POST" }),
};

export const paymentApi = {
  /*
    The Idempotency-Key header is what makes paying safe to retry. The backend remembers the key:
    sending the SAME key again (double click, flaky network) returns the same payment instead of
    charging twice. A NEW attempt needs a NEW key. PaymentPanel manages that.
  */
  pay: (orderId: number, idempotencyKey: string) =>
    apiFetch<Payment>("/payments", {
      method: "POST",
      body: { orderId },
      headers: { "Idempotency-Key": idempotencyKey },
    }),
};
