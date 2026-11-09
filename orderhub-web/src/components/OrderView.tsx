import { formatDateTime } from "../lib/format";
import type { Order, OrderStatus } from "../types";
import { Money, OrderStatusBadge } from "./ui";

/*
  OrderView shows an order: its progress, items, total and delivery address.
  Both the customer page and the admin page use it, so the two never drift apart.
*/
export function OrderView({ order }: { order: Order }) {
  return (
    <div className="space-y-6">
      <div className="panel p-5">
        <div className="mb-5 flex flex-wrap items-center justify-between gap-2">
          <h2 className="text-lg">Progress</h2>
          <OrderStatusBadge status={order.status} />
        </div>
        <OrderTimeline status={order.status} />
        <p className="mt-4 text-sm text-muted">Placed {formatDateTime(order.createdAt)}</p>
      </div>

      <div className="panel">
        <table className="w-full text-sm">
          <thead className="border-b border-line text-left text-muted">
            <tr>
              <th className="p-3 font-medium">Item</th>
              <th className="p-3 text-right font-medium">Price</th>
              <th className="p-3 text-right font-medium">Qty</th>
              <th className="p-3 text-right font-medium">Total</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-line">
            {order.items.map((item) => (
              <tr key={item.productId}>
                <td className="p-3 font-medium">{item.productName}</td>
                <td className="p-3 text-right"><Money value={item.unitPrice} /></td>
                <td className="num p-3 text-right">{item.quantity}</td>
                <td className="p-3 text-right"><Money value={item.lineTotal} /></td>
              </tr>
            ))}
          </tbody>
          <tfoot className="border-t border-line">
            <tr>
              <td colSpan={3} className="p-3 text-right font-semibold">Order total</td>
              <td className="p-3 text-right text-base font-bold"><Money value={order.totalAmount} /></td>
            </tr>
          </tfoot>
        </table>
      </div>

      <div className="panel p-5 text-sm">
        <h2 className="mb-2 text-lg">Delivery address</h2>
        {order.shippingAddress ? (
          <address className="not-italic text-muted">
            <p className="font-semibold text-ink">{order.shippingAddress.name}</p>
            <p>{order.shippingAddress.line1}{order.shippingAddress.line2 ? `, ${order.shippingAddress.line2}` : ""}</p>
            <p>{order.shippingAddress.city}, {order.shippingAddress.state} {order.shippingAddress.postalCode}, {order.shippingAddress.country}</p>
            <p>{order.shippingAddress.phone}</p>
          </address>
        ) : (
          <p className="text-muted">No address was saved with this order.</p>
        )}
      </div>
    </div>
  );
}

/*
  THE ORDER TIMELINE
  An order really is a sequence (placed, paid, shipped, delivered), so this is the one place the
  design shows numbered progress. A cancelled order leaves the sequence, so it gets a message instead.
*/
const STEPS: { status: OrderStatus; label: string }[] = [
  { status: "CREATED", label: "Placed" },
  { status: "PAID", label: "Paid" },
  { status: "SHIPPED", label: "Shipped" },
  { status: "DELIVERED", label: "Delivered" },
];

export function OrderTimeline({ status }: { status: OrderStatus }) {
  if (status === "CANCELLED") {
    return <p className="rounded-md bg-bad-soft px-3 py-2 text-sm text-bad">This order was cancelled.</p>;
  }

  const reached = STEPS.findIndex((s) => s.status === status); // index of the latest completed step
  return (
    <ol className="grid grid-cols-4 gap-2">
      {STEPS.map((step, index) => {
        const done = index <= reached;
        const waiting = index === reached + 1; // the step the order is waiting on
        return (
          <li key={step.status} className="relative">
            {/* the line that joins this step to the next one */}
            {index < STEPS.length - 1 && (
              <span aria-hidden="true" className={`absolute left-4 top-4 h-0.5 w-full ${index < reached ? "bg-pallet" : "bg-line"}`} />
            )}
            <span
              className={`relative flex h-8 w-8 items-center justify-center rounded-full text-sm font-bold ${
                done ? "bg-pallet text-white"
                : waiting ? "border-2 border-signal bg-signal-soft text-[#7a5b00]"
                : "border border-line bg-surface text-muted"
              }`}
            >
              {done ? "✓" : index + 1}
            </span>
            <p className={`mt-2 text-sm ${done ? "font-semibold" : "text-muted"}`}>{step.label}</p>
          </li>
        );
      })}
    </ol>
  );
}
