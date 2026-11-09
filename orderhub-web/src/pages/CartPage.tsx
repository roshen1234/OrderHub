import { Link } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { cartApi } from "../api/cart";
import { EmptyState, ErrorMessage, Loading, Money, PageHeader } from "../components/ui";

export function CartPage() {
  const queryClient = useQueryClient();
  const cart = useQuery({ queryKey: ["cart"], queryFn: cartApi.get });

  // Both mutations do the same thing afterwards: reload the cart.
  const refresh = () => queryClient.invalidateQueries({ queryKey: ["cart"] });
  const update = useMutation({
    mutationFn: ({ itemId, quantity }: { itemId: number; quantity: number }) => cartApi.updateItem(itemId, quantity),
    onSuccess: refresh,
  });
  const remove = useMutation({ mutationFn: (itemId: number) => cartApi.removeItem(itemId), onSuccess: refresh });

  if (cart.isPending) return <Loading />;
  if (cart.error) return <ErrorMessage error={cart.error} />;

  const { items, total } = cart.data;
  if (items.length === 0) {
    return (
      <>
        <PageHeader title="Your cart" />
        <EmptyState title="Your cart is empty">
          Find something in the <Link to="/products" className="link">shop</Link>.
        </EmptyState>
      </>
    );
  }

  const busy = update.isPending || remove.isPending;
  return (
    <div>
      <PageHeader title="Your cart" />
      <div className="mb-4 space-y-2">
        <ErrorMessage error={update.error ?? remove.error} />
      </div>

      <div className="grid gap-6 lg:grid-cols-[1fr_20rem]">
        <ul className="panel divide-y divide-line">
          {items.map((item) => (
            <li key={item.id} className="flex flex-wrap items-center gap-4 p-4">
              <div className="min-w-0 flex-1">
                <Link to={`/products/${item.productId}`} className="font-semibold hover:text-pallet">{item.productName}</Link>
                <p className="text-sm text-muted"><Money value={item.unitPrice} /> each</p>
              </div>

              <div className="flex items-center gap-1" role="group" aria-label={`Quantity for ${item.productName}`}>
                <button className="btn btn-secondary px-3" disabled={busy || item.quantity <= 1}
                        aria-label="Decrease quantity"
                        onClick={() => update.mutate({ itemId: item.id, quantity: item.quantity - 1 })}>−</button>
                <span className="num w-8 text-center font-semibold">{item.quantity}</span>
                <button className="btn btn-secondary px-3" disabled={busy}
                        aria-label="Increase quantity"
                        onClick={() => update.mutate({ itemId: item.id, quantity: item.quantity + 1 })}>+</button>
              </div>

              <p className="w-28 text-right font-semibold"><Money value={item.lineTotal} /></p>
              <button className="btn btn-danger" disabled={busy} onClick={() => remove.mutate(item.id)}>Remove</button>
            </li>
          ))}
        </ul>

        <aside className="panel h-fit p-5">
          <h2 className="text-lg">Order total</h2>
          <p className="mt-2 text-3xl font-bold"><Money value={total} /></p>
          <p className="mt-1 text-xs text-muted">Prices are locked in when you place the order.</p>
          <Link to="/checkout" className="btn btn-primary mt-5 w-full">Continue to checkout</Link>
        </aside>
      </div>
    </div>
  );
}
