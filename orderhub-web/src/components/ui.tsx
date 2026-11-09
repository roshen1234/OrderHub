import type { ReactNode } from "react";
import { ApiError } from "../api/client";
import { formatMoney } from "../lib/format";
import type { OrderStatus, PaymentStatus } from "../types";

/* Small presentational components that are used on many pages. */

export function Money({ value }: { value: number }) {
  return <span className="num">{formatMoney(value)}</span>;
}

/** Shows an error from the API in plain words. Pass anything caught by react-query. */
export function ErrorMessage({ error }: { error: unknown }) {
  if (!error) return null;
  const text = error instanceof ApiError || error instanceof Error ? error.message : "Something went wrong.";
  return (
    <p role="alert" className="rounded-md border border-bad/30 bg-bad-soft px-3 py-2 text-sm text-bad">
      {text}
    </p>
  );
}

export function SuccessMessage({ children }: { children: ReactNode }) {
  return (
    <p role="status" className="rounded-md border border-ok/30 bg-ok-soft px-3 py-2 text-sm text-ok">
      {children}
    </p>
  );
}

export function Loading({ label = "Loading…" }: { label?: string }) {
  return <p className="py-8 text-muted">{label}</p>;
}

/** An empty list is an invitation to act, so it always says what to do next. */
export function EmptyState({ title, children }: { title: string; children?: ReactNode }) {
  return (
    <div className="panel px-6 py-10 text-center">
      <h2 className="text-lg">{title}</h2>
      {children && <div className="mt-2 text-sm text-muted">{children}</div>}
    </div>
  );
}

export function PageHeader({ title, children }: { title: string; children?: ReactNode }) {
  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
      <h1 className="text-3xl">{title}</h1>
      {children}
    </div>
  );
}

// ---------- status badges ----------

const orderStyles: Record<OrderStatus, string> = {
  CREATED: "bg-signal-soft text-[#7a5b00]",
  PAID: "bg-pallet-soft text-pallet-dark",
  SHIPPED: "bg-pallet-soft text-pallet-dark",
  DELIVERED: "bg-ok-soft text-ok",
  CANCELLED: "bg-bad-soft text-bad",
};
const orderLabels: Record<OrderStatus, string> = {
  CREATED: "Awaiting payment",
  PAID: "Paid",
  SHIPPED: "Shipped",
  DELIVERED: "Delivered",
  CANCELLED: "Cancelled",
};

export function OrderStatusBadge({ status }: { status: OrderStatus }) {
  return (
    <span className={`inline-block rounded px-2 py-0.5 text-xs font-semibold ${orderStyles[status]}`}>
      {orderLabels[status]}
    </span>
  );
}

const paymentLabels: Record<PaymentStatus, string> = {
  PENDING: "Waiting for confirmation",
  SUCCESS: "Payment received",
  FAILED: "Payment failed",
  REFUND_PENDING: "Refund on its way",
  REFUNDED: "Refunded",
};
export const paymentLabel = (status: PaymentStatus) => paymentLabels[status];

// ---------- pagination ----------

export function Pagination({
  page,
  totalPages,
  onChange,
}: {
  page: number; // zero-based, like the backend
  totalPages: number;
  onChange: (page: number) => void;
}) {
  if (totalPages <= 1) return null;
  return (
    <nav aria-label="Pagination" className="mt-6 flex items-center justify-between">
      <button className="btn btn-secondary" disabled={page === 0} onClick={() => onChange(page - 1)}>
        Previous
      </button>
      <span className="num text-sm text-muted">
        Page {page + 1} of {totalPages}
      </span>
      <button className="btn btn-secondary" disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)}>
        Next
      </button>
    </nav>
  );
}
