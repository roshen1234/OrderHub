import { useState } from "react";
import { Link } from "react-router-dom";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { orderApi } from "../api/orders";
import { EmptyState, ErrorMessage, Loading, Money, OrderStatusBadge, PageHeader, Pagination } from "../components/ui";
import { formatDateTime } from "../lib/format";

export function OrdersPage() {
  const [page, setPage] = useState(0);
  const orders = useQuery({
    queryKey: ["orders", page],
    queryFn: () => orderApi.list(page),
    placeholderData: keepPreviousData,
  });

  return (
    <div>
      <PageHeader title="Your orders" />
      <ErrorMessage error={orders.error} />
      {orders.isPending ? <Loading /> : orders.data?.content.length === 0 ? (
        <EmptyState title="You have not placed an order yet">
          Browse the <Link to="/products" className="link">shop</Link> and your orders will show up here.
        </EmptyState>
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
                </tr>
              </thead>
              <tbody className="divide-y divide-line">
                {orders.data?.content.map((o) => (
                  <tr key={o.id} className="hover:bg-paper">
                    <td className="p-3"><Link to={`/orders/${o.id}`} className="link font-semibold num">#{o.id}</Link></td>
                    <td className="p-3 text-muted">{formatDateTime(o.createdAt)}</td>
                    <td className="p-3"><OrderStatusBadge status={o.status} /></td>
                    <td className="p-3 text-right"><Money value={o.totalAmount} /></td>
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
