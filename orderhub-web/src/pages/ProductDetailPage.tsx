import { useState } from "react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { catalogApi } from "../api/catalog";
import { cartApi } from "../api/cart";
import { useAuth } from "../auth/AuthContext";
import { ApiError } from "../api/client";
import { ErrorMessage, Loading, Money, SuccessMessage } from "../components/ui";
import { ProductPlate } from "./ProductsPage";

export function ProductDetailPage() {
  const { id } = useParams(); // the ":id" part of /products/:id (always a string)
  const productId = Number(id);
  const { user } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const queryClient = useQueryClient();
  const [quantity, setQuantity] = useState(1);

  const product = useQuery({
    queryKey: ["product", productId],
    queryFn: () => catalogApi.getProduct(productId),
    enabled: Number.isFinite(productId),
  });

  const addToCart = useMutation({
    mutationFn: () => cartApi.addItem(productId, quantity),
    // After the cart changed on the server, mark the cached cart as outdated so it reloads.
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["cart"] }),
  });

  function handleAdd() {
    if (!user) {
      // Guests can browse, but need an account to buy. Send them to sign in, then bring them back here.
      navigate("/login", { state: { from: location.pathname } });
      return;
    }
    addToCart.mutate();
  }

  if (product.isPending) return <Loading />;
  if (product.error) {
    const notFound = product.error instanceof ApiError && product.error.status === 404;
    return (
      <div>
        <ErrorMessage error={notFound ? new Error("This product is no longer available.") : product.error} />
        <Link to="/products" className="link mt-4 inline-block">Back to the shop</Link>
      </div>
    );
  }

  const p = product.data;
  return (
    <div className="grid gap-8 md:grid-cols-2">
      <div className="panel overflow-hidden"><ProductPlate product={p} large /></div>

      <div>
        <Link to="/products" className="link text-sm">All products</Link>
        <p className="mt-4 text-sm text-muted">{p.categoryName}</p>
        <h1 className="mt-1 text-3xl">{p.name}</h1>
        <p className="mt-4 text-2xl font-semibold"><Money value={p.price} /></p>
        {p.description && <p className="mt-4 max-w-prose text-muted">{p.description}</p>}

        <div className="mt-6 flex items-end gap-3">
          <div>
            <label htmlFor="qty" className="label">Quantity</label>
            <input id="qty" type="number" min={1} max={99} className="input w-24" value={quantity}
                   onChange={(e) => setQuantity(Math.max(1, Number(e.target.value) || 1))} />
          </div>
          <button className="btn btn-primary" onClick={handleAdd} disabled={addToCart.isPending}>
            {addToCart.isPending ? "Adding…" : "Add to cart"}
          </button>
        </div>

        <div className="mt-4 space-y-3">
          <ErrorMessage error={addToCart.error} />
          {addToCart.isSuccess && (
            <SuccessMessage>
              Added to your cart. <Link to="/cart" className="font-semibold underline">View cart</Link>
            </SuccessMessage>
          )}
          <p className="text-xs text-muted">Stock is checked and reserved when you place the order.</p>
        </div>
      </div>
    </div>
  );
}
