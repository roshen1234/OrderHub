import { useRef, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { orderApi, paymentApi } from "../api/orders";
import { ApiError } from "../api/client";
import { OrderView } from "../components/OrderView";
import { ErrorMessage, Loading, Money, PageHeader, SuccessMessage, paymentLabel } from "../components/ui";
import { formatCountdown, useCountdown } from "../lib/useCountdown";
import { newIdempotencyKey } from "../lib/format";
import type { Payment } from "../types";

export function OrderDetailPage() {
  const { id } = useParams();
  const orderId = Number(id);
  const queryClient = useQueryClient();
  // Once a payment has been started, ask the server for the order every 3 seconds until it is paid.
  const [watchingPayment, setWatchingPayment] = useState(false);

  const order = useQuery({
    queryKey: ["order", orderId],
    queryFn: () => orderApi.get(orderId),
    // refetchInterval can be a function: returning false turns polling off.
    refetchInterval: (query) => (watchingPayment && query.state.data?.status === "CREATED" ? 3000 : false),
  });

  const cancel = useMutation({
    mutationFn: () => orderApi.cancel(orderId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["order", orderId] });
      queryClient.invalidateQueries({ queryKey: ["orders"] });
    },
  });

  const awaitingPayment = order.data?.status === "CREATED";
  const secondsLeft = useCountdown(order.data?.expiresAt, awaitingPayment);

  if (order.isPending) return <Loading />;
  if (order.error) {
    const notFound = order.error instanceof ApiError && order.error.status === 404;
    return (
      <div className="space-y-3">
        <ErrorMessage error={notFound ? new Error("We could not find that order.") : order.error} />
        <Link to="/orders" className="link">Back to your orders</Link>
      </div>
    );
  }

  const o = order.data;
  const expired = secondsLeft === 0;
  const canCancel = o.status === "CREATED" || o.status === "PAID"; // the backend rejects it after shipping

  return (
    <div>
      <PageHeader title={`Order #${o.id}`}>
        <Link to="/orders" className="link text-sm">All orders</Link>
      </PageHeader>

      <div className="grid gap-6 lg:grid-cols-[1fr_22rem]">
        <OrderView order={o} />

        <aside className="space-y-4">
          {awaitingPayment && (
            expired ? (
              <div className="panel p-5 text-sm">
                <h2 className="text-lg">Reservation ended</h2>
                <p className="mt-2 text-muted">The time to pay has run out. The order is being cancelled and the items go back on sale.</p>
              </div>
            ) : (
              <PaymentPanel
                orderId={o.id}
                amount={o.totalAmount}
                countdown={secondsLeft}
                onStarted={() => setWatchingPayment(true)}
              />
            )
          )}

          {o.status === "PAID" && <SuccessMessage>Payment received. We will ship your order soon.</SuccessMessage>}

          {canCancel && (
            <div className="panel space-y-3 p-5">
              <h2 className="text-lg">Cancel this order</h2>
              <p className="text-sm text-muted">
                {o.status === "PAID" ? "Your payment will be refunded." : "The reserved items go back on sale."}
              </p>
              <ErrorMessage error={cancel.error} />
              <button className="btn btn-danger" disabled={cancel.isPending}
                      onClick={() => { if (window.confirm("Cancel this order?")) cancel.mutate(); }}>
                {cancel.isPending ? "Cancelling…" : "Cancel order"}
              </button>
            </div>
          )}
        </aside>
      </div>
    </div>
  );
}

/*
  PAYMENT PANEL
  The key idea is the idempotency key. Each payment ATTEMPT gets one random key, kept in a ref
  (a value that survives re-renders without causing one). While the key stays the same, sending
  the request again - a double click, a retry after a timeout - makes the server return the SAME
  payment instead of charging twice. When an attempt is finished or rejected, we make a new key.
*/
function PaymentPanel({
  orderId, amount, countdown, onStarted,
}: { orderId: number; amount: number; countdown: number | null; onStarted: () => void }) {
  const keyRef = useRef(newIdempotencyKey());
  const [payment, setPayment] = useState<Payment | null>(null);

  const pay = useMutation({
    mutationFn: () => paymentApi.pay(orderId, keyRef.current),
    onSuccess: (result) => {
      setPayment(result);
      if (result.status === "FAILED") keyRef.current = newIdempotencyKey(); // failed: the next try is a new attempt
      onStarted();
    },
    onError: (error) => {
      // A 4xx means the server rejected this attempt, so the next one should use a fresh key.
      // No status at all (network error) means we do not know what happened: KEEP the key and retry safely.
      if (error instanceof ApiError) keyRef.current = newIdempotencyKey();
    },
  });

  const waiting = payment?.status === "PENDING";

  return (
    <div className="panel space-y-3 p-5">
      <h2 className="text-lg">Pay for this order</h2>
      <p className="text-2xl font-bold"><Money value={amount} /></p>
      {countdown !== null && (
        <p className="rounded-md bg-signal-soft px-3 py-2 text-sm text-[#7a5b00]">
          Items are reserved for <span className="num font-semibold">{formatCountdown(countdown)}</span>
        </p>
      )}

      <ErrorMessage error={pay.error} />

      {payment && (
        <p className={`rounded-md px-3 py-2 text-sm ${payment.status === "FAILED" ? "bg-bad-soft text-bad" : "bg-pallet-soft text-pallet-dark"}`}>
          {paymentLabel(payment.status)}
          {payment.status === "FAILED" && ". You can try again."}
        </p>
      )}

      {waiting && payment?.providerRef && import.meta.env.DEV && (
        // Only shown in development (npm run dev). With the mock gateway nobody pays on a bank page,
        // so an admin confirms it under Admin > Payment simulator using this reference.
        <p className="rounded-md border border-dashed border-line p-3 text-xs text-muted">
          Development hint: confirm this payment in Admin, Payment simulator, with reference{" "}
          <code className="break-all font-mono text-ink">{payment.providerRef}</code>
        </p>
      )}

      <button className="btn btn-primary w-full" disabled={pay.isPending || waiting} onClick={() => pay.mutate()}>
        {pay.isPending ? "Starting payment…" : waiting ? "Waiting for confirmation…" : "Pay now"}
      </button>
    </div>
  );
}
