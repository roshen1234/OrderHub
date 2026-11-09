import { Navigate, Outlet, Route, Routes } from "react-router-dom";
import { Navbar } from "./components/Navbar";
import { ProtectedRoute } from "./auth/ProtectedRoute";
import { LoginPage, RegisterPage } from "./pages/AuthPages";
import { ProductsPage } from "./pages/ProductsPage";
import { ProductDetailPage } from "./pages/ProductDetailPage";
import { CartPage } from "./pages/CartPage";
import { CheckoutPage } from "./pages/CheckoutPage";
import { AddressesPage } from "./pages/AddressesPage";
import { OrdersPage } from "./pages/OrdersPage";
import { OrderDetailPage } from "./pages/OrderDetailPage";
import { AdminLayout } from "./pages/admin/AdminLayout";
import { AdminProductsPage } from "./pages/admin/AdminProductsPage";
import { AdminOrdersPage } from "./pages/admin/AdminOrdersPage";
import { AdminOrderDetailPage } from "./pages/admin/AdminOrderDetailPage";
import { AdminToolsPage } from "./pages/admin/AdminToolsPage";

/* The page frame: the navbar on top, and the current page rendered where <Outlet /> is. */
function Layout() {
  return (
    <>
      <Navbar />
      <main className="mx-auto max-w-6xl px-4 py-8">
        <Outlet />
      </main>
    </>
  );
}

/*
  ROUTES
  Each <Route> maps a URL to a page component. Routes nested inside another route share its layout.
    Layout
     ├─ public pages        (anyone)
     ├─ ProtectedRoute      (signed-in users)
     └─ ProtectedRoute requireAdmin -> AdminLayout   (admins)
*/
export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Navigate to="/products" replace />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/products" element={<ProductsPage />} />
        <Route path="/products/:id" element={<ProductDetailPage />} />

        <Route element={<ProtectedRoute />}>
          <Route path="/cart" element={<CartPage />} />
          <Route path="/checkout" element={<CheckoutPage />} />
          <Route path="/addresses" element={<AddressesPage />} />
          <Route path="/orders" element={<OrdersPage />} />
          <Route path="/orders/:id" element={<OrderDetailPage />} />
        </Route>

        <Route element={<ProtectedRoute requireAdmin />}>
          <Route path="/admin" element={<AdminLayout />}>
            <Route index element={<Navigate to="products" replace />} />
            <Route path="products" element={<AdminProductsPage />} />
            <Route path="orders" element={<AdminOrdersPage />} />
            <Route path="orders/:id" element={<AdminOrderDetailPage />} />
            <Route path="tools" element={<AdminToolsPage />} />
          </Route>
        </Route>

        <Route
          path="*"
          element={
            <div>
              <h1 className="text-2xl">Page not found</h1>
              <p className="mt-2 text-muted">Check the address, or go back to the shop.</p>
            </div>
          }
        />
      </Route>
    </Routes>
  );
}
