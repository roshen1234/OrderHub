import { useState } from "react";
import { Link } from "react-router-dom";
import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { adminApi } from "../../api/admin";
import { EmptyState, ErrorMessage, Loading, Money, OrderStatusBadge, Pagination } from "../../components/ui";
import { formatDateTime } from "../../lib/format";
import type { OrderStatus } from "../../types";

const STATUSES: OrderStatus[] = ["CREATED", "PAID", "SHIPPED", "DELIVERED", "CANCELLED"];

export function AdminOrdersPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [status, setStatus] = useState("");

  // The status filter is sent as ?status=PAID. If your backend list endpoint ignores it,
  // the list simply shows every order.
  const orders = useQuery({
    queryKey: ["admin-orders", status, page],
    queryFn: () => adminApi.listOrders({ status, page }),
    placeholderData: keepPreviousData,
  });

  const refresh = () => queryClient.invalidateQueries({ queryKey: ["admin-orders"] });
  const ship = useMutation({ mutationFn: (id: number) => adminApi.shipOrder(id), onSuccess: refresh });
  const deliver = useMutation({ mutationFn: (id: number) => adminApi.deliverOrder(id), onSuccess: refresh });
  const busy = ship.isPending || deliver.isPending;

  return (
    <div className="space-y-4">
      <div className="flex items-end gap-3">
        <div>
          <label htmlFor="status" className="label">Status</label>
          <select id="status" className="input w-48" value={status} onChange={(e) => { setStatus(e.target.value); setPage(0); }}>
            <option value="">All orders</option>
            {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
          </select>
        </div>
      </div>

      <ErrorMessage error={orders.error ?? ship.error ?? deliver.error} />

      {orders.isPending ? <Loading /> : orders.data?.content.length === 0 ? (
        <EmptyState title="No orders found" />
      ) : (
        <>
          <div className="panel overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="border-b border-line text-left text-muted">
                <tr>
                  <th className="p-3 font-medium">Order</th>
                  <th className="p-3 font-medium">Placed</th>
                  <th className="p-3 font-medium">Status</th>
                  <th className="p-3 text-right font-medium">Total</th>
                  <th className="p-3 font-medium"><span className="sr-only">Actions</span></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-line">
                {orders.data?.content.map((o) => (
                  <tr key={o.id}>
                    <td className="p-3"><Link to={`/admin/orders/${o.id}`} className="link num font-semibold">#{o.id}</Link></td>
                    <td className="p-3 text-muted">{formatDateTime(o.createdAt)}</td>
                    <td className="p-3"><OrderStatusBadge status={o.status} /></td>
                    <td className="p-3 text-right"><Money value={o.totalAmount} /></td>
                    <td className="p-3 text-right">
                      {/* Only offer the step that is allowed next. The backend enforces the same rules. */}
                      {o.status === "PAID" && <button className="btn btn-primary" disabled={busy} onClick={() => ship.mutate(o.id)}>Ship</button>}
                      {o.status === "SHIPPED" && <button className="btn btn-primary" disabled={busy} onClick={() => deliver.mutate(o.id)}>Mark delivered</button>}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pagination page={page} totalPages={orders.data?.totalPages ?? 0} onChange={setPage} />
        </>
      )}
    </div>
  );
}
