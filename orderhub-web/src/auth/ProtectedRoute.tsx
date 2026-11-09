import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "./AuthContext";

/*
  PROTECTED ROUTES
  Wraps pages that need a signed-in user (and optionally an admin).
  Remember: this only decides what the BROWSER shows. The backend still checks the token and the
  role on every request, so hiding a page here is a convenience, not security.
*/
export function ProtectedRoute({ requireAdmin = false }: { requireAdmin?: boolean }) {
  const { user, isAdmin, initializing } = useAuth();
  const location = useLocation();

  // Still restoring the session: do not redirect yet, or a reload would always bounce to /login.
  if (initializing) return <p className="p-8 text-muted">Loading…</p>;

  // Not signed in: go to the login page and remember where the user wanted to go.
  if (!user) return <Navigate to="/login" state={{ from: location.pathname + location.search }} replace />;

  if (requireAdmin && !isAdmin) {
    return (
      <div className="mx-auto max-w-xl p-8">
        <h1 className="text-2xl">Admins only</h1>
        <p className="mt-2 text-muted">Your account does not have access to this area.</p>
      </div>
    );
  }

  return <Outlet />; // render the matching child route
}
