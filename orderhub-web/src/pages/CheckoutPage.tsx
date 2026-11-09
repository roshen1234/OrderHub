import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { cartApi } from "../api/cart";
import { addressApi } from "../api/addresses";
import { orderApi } from "../api/orders";
import { EmptyState, ErrorMessage, Loading, Money, PageHeader } from "../components/ui";

export function CheckoutPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const cart = useQuery({ queryKey: ["cart"], queryFn: cartApi.get });
  const addresses = useQuery({ queryKey: ["addresses"], queryFn: addressApi.list });
  const [chosenId, setChosenId] = useState<number | null>(null);

  const placeOrder = useMutation({
    mutationFn: (addressId: number) => orderApi.checkout(addressId),
    onSuccess: (order) => {
      // Checkout empties the cart and reserves stock on the server, so refresh what we cached.
      queryClient.invalidateQueries({ queryKey: ["cart"] });
      queryClient.invalidateQueries({ queryKey: ["orders"] });
      navigate(`/orders/${order.id}`, { replace: true }); // next step: pay
    },
  });

  if (cart.isPending || addresses.isPending) return <Loading />;
  if (cart.error || addresses.error) return <ErrorMessage error={cart.error ?? addresses.error} />;

  if (cart.data.items.length === 0) {
    return (
      <EmptyState title="There is nothing to check out">
        Add something from the <Link to="/products" className="link">shop</Link> first.
      </EmptyState>
    );
  }
  if (addresses.data.length === 0) {
    return (
      <EmptyState title="Add a delivery address first">
        We need to know where to send your order. <Link to="/addresses" className="link">Add an address</Link>, then come back.
      </EmptyState>
    );
  }

  // Derived state: the user's pick, otherwise their default address, otherwise the first one.
  const selectedId = chosenId ?? addresses.data.find((a) => a.defaultAddress)?.id ?? addresses.data[0].id;

  return (
    <div>
      <PageHeader title="Checkout" />
      <div className="grid gap-6 lg:grid-cols-[1fr_20rem]">
        <section>
          <h2 className="mb-3 text-lg">Deliver to</h2>
          <ul className="space-y-3">
            {addresses.data.map((a) => (
              <li key={a.id}>
                <label className={`panel flex cursor-pointer gap-3 p-4 ${selectedId === a.id ? "border-pallet ring-1 ring-pallet" : ""}`}>
                  <input type="radio" name="address" className="mt-1" checked={selectedId === a.id} onChange={() => setChosenId(a.id)} />
                  <span className="text-sm">
                    <span className="block font-semibold">{a.recipientName}{a.label ? ` (${a.label})` : ""}</span>
                    <span className="block text-muted">{a.line1}{a.line2 ? `, ${a.line2}` : ""}</span>
                    <span className="block text-muted">{a.city}, {a.state} {a.postalCode}, {a.country}</span>
                    <span className="block text-muted">{a.phone}</span>
                  </span>
                </label>
              </li>
            ))}
          </ul>
          <Link to="/addresses" className="link mt-3 inline-block text-sm">Manage addresses</Link>
        </section>

        <aside className="panel h-fit p-5">
          <h2 className="text-lg">Summary</h2>
          <ul className="mt-3 space-y-1 text-sm">
            {cart.data.items.map((i) => (
              <li key={i.id} className="flex justify-between gap-3">
                <span>{i.quantity} × {i.productName}</span>
                <Money value={i.lineTotal} />
              </li>
            ))}
          </ul>
          <p className="mt-4 flex justify-between border-t border-line pt-3 text-lg font-bold">
            <span>Total</span><Money value={cart.data.total} />
          </p>

          <div className="mt-4 space-y-3">
            <ErrorMessage error={placeOrder.error} />
            {/* Disabling the button while the request runs stops accidental double clicks.
                The backend also guards against a double submit, but good UI helps too. */}
            <button className="btn btn-primary w-full" disabled={placeOrder.isPending} onClick={() => placeOrder.mutate(selectedId)}>
              {placeOrder.isPending ? "Placing order…" : "Place order"}
            </button>
            <p className="text-xs text-muted">Your items are reserved for a short time while you pay.</p>
          </div>
        </aside>
      </div>
    </div>
  );
}
