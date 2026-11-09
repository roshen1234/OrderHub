import type { Category, PageResponse, Product } from "../types";
import { apiFetch } from "./client";

export interface ProductQuery {
  name?: string;
  categoryId?: string;
  minPrice?: string;
  maxPrice?: string;
  page?: number;
  size?: number;
  sort?: string; // for example "price,asc"
}

/** Builds "?a=1&b=2" and leaves out empty values, so the backend only sees filters that were set. */
export function toQueryString(params: Record<string, string | number | undefined>): string {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== "") search.set(key, String(value));
  }
  const text = search.toString();
  return text ? `?${text}` : "";
}

export const catalogApi = {
  listProducts: (query: ProductQuery) =>
    apiFetch<PageResponse<Product>>(`/products${toQueryString({ ...query })}`, { auth: false }),

  getProduct: (id: number) => apiFetch<Product>(`/products/${id}`, { auth: false }),

  listCategories: () => apiFetch<Category[]>("/categories", { auth: false }),
};
