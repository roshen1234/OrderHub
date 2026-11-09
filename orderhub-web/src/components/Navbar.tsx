import { Link, NavLink, useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { cartApi } from "../api/cart";
import { useAuth } from "../auth/AuthContext";

/* The top bar. The cart count is a normal query, so it stays in sync whenever the cart changes. */
export function Navbar() {
  const { user, isAdmin, logout } = useAuth();
  const navigate = useNavigate();

  // "enabled" means: only ask the backend for the cart when someone is signed in.
  const cart = useQuery({ queryKey: ["cart"], queryFn: cartApi.get, enabled: !!user });
  const itemCount = cart.data?.items.reduce((sum, item) => sum + item.quantity, 0) ?? 0;

  // NavLink gives the active page a different style.
  const linkClass = ({ isActive }: { isActive: boolean }) =>
    `px-1 py-1 text-sm font-medium border-b-2 ${isActive ? "border-pallet text-ink" : "border-transparent text-muted hover:text-ink"}`;

  return (
    <header className="border-b border-line bg-surface">
      <div className="mx-auto flex max-w-6xl flex-wrap items-center gap-x-6 gap-y-2 px-4 py-3">
        <Link to="/products" className="text-xl font-bold tracking-tight" style={{ fontStretch: "118%" }}>
          OrderHub
        </Link>

        <nav className="flex flex-wrap items-center gap-5">
          <NavLink to="/products" className={linkClass}>Shop</NavLink>
          {user && (
            <>
              <NavLink to="/orders" className={linkClass}>Orders</NavLink>
              <NavLink to="/addresses" className={linkClass}>Addresses</NavLink>
            </>
          )}
          {isAdmin && <NavLink to="/admin" className={linkClass}>Admin</NavLink>}
        </nav>

        <div className="ml-auto flex items-center gap-4">
          {user ? (
            <>
              <Link to="/cart" className="text-sm font-medium hover:text-pallet">
                Cart <span className="num rounded bg-pallet-soft px-1.5 py-0.5 text-xs text-pallet-dark">{itemCount}</span>
              </Link>
              <span className="hidden text-sm text-muted sm:inline">{user.email}</span>
              <button
                className="btn btn-secondary"
                onClick={async () => {
                  await logout();
                  navigate("/products");
                }}
              >
                Sign out
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className="text-sm font-medium hover:text-pallet">Sign in</Link>
              <Link to="/register" className="btn btn-primary">Create account</Link>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
