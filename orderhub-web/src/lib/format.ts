/*
  Small formatting helpers so every page shows money and dates the same way.
  Currency and locale are configurable in a .env file (see .env.example).
*/
const CURRENCY = import.meta.env.VITE_CURRENCY ?? "INR";
const LOCALE = import.meta.env.VITE_LOCALE ?? "en-IN";

const moneyFormatter = new Intl.NumberFormat(LOCALE, { style: "currency", currency: CURRENCY });
export const formatMoney = (value: number) => moneyFormatter.format(value);

/**
 * The backend sends LocalDateTime values like "2026-10-11T19:00:00" (no time zone), which the
 * browser reads as local time. That is correct while browser and server share a time zone.
 * A production API would send an Instant (ending in "Z") to avoid any ambiguity.
 */
export const formatDateTime = (iso: string) =>
  new Date(iso).toLocaleString(LOCALE, { dateStyle: "medium", timeStyle: "short" });

/** A random key used as the Idempotency-Key header for one payment attempt. */
export function newIdempotencyKey(): string {
  // crypto.randomUUID needs a secure context (https or localhost), so keep a fallback.
  if (typeof crypto !== "undefined" && "randomUUID" in crypto) return crypto.randomUUID();
  return Math.random().toString(36).slice(2) + Date.now().toString(36);
}
