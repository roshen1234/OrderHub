import type { Address, AddressInput } from "../types";
import { apiFetch } from "./client";

export const addressApi = {
  list: () => apiFetch<Address[]>("/addresses"),
  create: (input: AddressInput) => apiFetch<Address>("/addresses", { method: "POST", body: input }),
  update: (id: number, input: AddressInput) => apiFetch<Address>(`/addresses/${id}`, { method: "PUT", body: input }),
  remove: (id: number) => apiFetch<void>(`/addresses/${id}`, { method: "DELETE" }),
};
