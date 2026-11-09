import { Link, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { adminApi } from "../../api/admin";
import { OrderView } from "../../components/OrderView";
import { ErrorMessage, Loading } from "../../components/ui";

export function AdminOrderDetailPage() {
  const { id } = useParams();
  const orderId = Number(id);
  const queryClient = useQueryClient();

  const order = useQuery({ queryKey: ["admin-order", orderId], queryFn: () => adminApi.getOrder(orderId) });

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ["admin-order", orderId] });
    queryClient.invalidateQueries({ queryKey: ["admin-orders"] });
  };
  const ship = useMutation({ mutationFn: () => adminApi.shipOrder(orderId), onSuccess: refresh });
  const deliver = useMutation({ mutationFn: () => adminApi.deliverOrder(orderId), onSuccess: refresh });

  if (order.isPending) return <Loading />;
  if (order.error) return <ErrorMessage error={order.error} />;
  const o = order.data;

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl">Order #{o.id}</h2>
        <Link to="/admin/orders" className="link text-sm">All orders</Link>
      </div>
      <ErrorMessage error={ship.error ?? deliver.error} />
      <div className="flex gap-2">
        {o.status === "PAID" && (
          <button className="btn btn-primary" disabled={ship.isPending} onClick={() => ship.mutate()}>
            {ship.isPending ? "Shipping…" : "Ship order"}
          </button>
        )}
        {o.status === "SHIPPED" && (
          <button className="btn btn-primary" disabled={deliver.isPending} onClick={() => deliver.mutate()}>Mark delivered</button>
        )}
      </div>
      <OrderView order={o} />
    </div>
  );
}
