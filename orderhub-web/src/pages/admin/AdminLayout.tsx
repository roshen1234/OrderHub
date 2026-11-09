import { NavLink, Outlet } from "react-router-dom";

/* The frame shared by every admin page: a title, tabs, and the current tab below. */
export function AdminLayout() {
  const tab = ({ isActive }: { isActive: boolean }) =>
    `rounded-md px-3 py-1.5 text-sm font-medium ${isActive ? "bg-ink text-white" : "text-muted hover:bg-line/60 hover:text-ink"}`;
  return (
    <div>
      <h1 className="text-3xl">Administration</h1>
      <nav className="mb-6 mt-4 flex flex-wrap gap-2 border-b border-line pb-3" aria-label="Admin sections">
        <NavLink to="/admin/products" className={tab}>Products and stock</NavLink>
        <NavLink to="/admin/orders" className={tab}>Orders</NavLink>
        <NavLink to="/admin/tools" className={tab}>Payment simulator</NavLink>
      </nav>
      <Outlet />
    </div>
  );
}
