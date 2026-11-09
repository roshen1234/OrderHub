import { useState, type FormEvent } from "react";
import { keepPreviousData, useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { adminApi } from "../../api/admin";
import { catalogApi } from "../../api/catalog";
import { ErrorMessage, Loading, Money, Pagination, SuccessMessage } from "../../components/ui";
import type { Category, Product, StockResponse } from "../../types";

export function AdminProductsPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [editing, setEditing] = useState<Product | null>(null);

  const categories = useQuery({ queryKey: ["categories"], queryFn: catalogApi.listCategories });
  const products = useQuery({
    queryKey: ["products", "admin", page],
    queryFn: () => catalogApi.listProducts({ page, size: 10, sort: "id,desc" }),
    placeholderData: keepPreviousData,
  });

  // Stock of the products on this page. It is a separate request because the product endpoint
  // does not include stock. If the backend has no GET /inventory endpoint yet, the columns show "-".
  const productIds = products.data?.content.map((p) => p.id) ?? [];
  const stocks = useQuery({
    queryKey: ["stock", productIds],
    queryFn: () => adminApi.getStocks(productIds),
    enabled: productIds.length > 0,
    retry: false,
  });
  const stockById = new Map((stocks.data ?? []).map((s) => [s.productId, s]));

  const deactivate = useMutation({
    mutationFn: (id: number) => adminApi.deactivateProduct(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["products"] }),
  });

  return (
    <div className="space-y-8">
      <div className="grid gap-6 lg:grid-cols-2">
        <ProductForm
          key={editing?.id ?? "new"} // switching between "new" and "edit" builds a fresh form
          product={editing}
          categories={categories.data ?? []}
          onDone={() => setEditing(null)}
        />
        <CategoryForm categories={categories.data ?? []} />
      </div>

      <section>
        <h2 className="mb-3 text-lg">Active products</h2>
        <div className="mb-3"><ErrorMessage error={products.error ?? deactivate.error} /></div>
        {products.isPending ? <Loading /> : (
          <>
            <div className="panel overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="border-b border-line text-left text-muted">
                  <tr>
                    <th className="p-3 font-medium">Product</th>
                    <th className="p-3 font-medium">Category</th>
                    <th className="p-3 text-right font-medium">Price</th>
                    <th className="p-3 text-right font-medium" title="Units customers can still buy">Available</th>
                    <th className="p-3 text-right font-medium" title="Units held for orders that have not shipped yet">Reserved</th>
                    <th className="p-3 font-medium">Adjust stock</th>
                    <th className="p-3 font-medium"><span className="sr-only">Actions</span></th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-line">
                  {products.data?.content.map((p) => (
                    <tr key={p.id} className="align-top">
                      <td className="p-3"><span className="num text-muted">#{p.id}</span> <span className="font-semibold">{p.name}</span></td>
                      <td className="p-3 text-muted">{p.categoryName}</td>
                      <td className="p-3 text-right"><Money value={p.price} /></td>
                      <StockCells stock={stockById.get(p.id)} loading={stocks.isLoading} />
                      <td className="p-3"><StockControl productId={p.id} /></td>
                      <td className="p-3">
                        <div className="flex gap-2">
                          <button className="btn btn-secondary" onClick={() => { setEditing(p); window.scrollTo({ top: 0 }); }}>Edit</button>
                          <button className="btn btn-danger" disabled={deactivate.isPending}
                                  onClick={() => { if (window.confirm(`Deactivate "${p.name}"? It disappears from the shop; old orders keep it.`)) deactivate.mutate(p.id); }}>
                            Deactivate
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination page={page} totalPages={products.data?.totalPages ?? 0} onChange={setPage} />
          </>
        )}
      </section>
    </div>
  );
}

/** Creates a product, or edits one when `product` is given. Stock is only set when creating. */
function ProductForm({ product, categories, onDone }: { product: Product | null; categories: Category[]; onDone: () => void }) {
  const queryClient = useQueryClient();
  const [name, setName] = useState(product?.name ?? "");
  const [description, setDescription] = useState(product?.description ?? "");
  const [price, setPrice] = useState(product ? String(product.price) : "");
  const [categoryId, setCategoryId] = useState(product ? String(product.categoryId) : "");
  const [initialStock, setInitialStock] = useState("0");

  const save = useMutation({
    mutationFn: () => {
      const input = { name: name.trim(), description: description.trim(), price: Number(price), categoryId: Number(categoryId) };
      return product ? adminApi.updateProduct(product.id, input) : adminApi.createProduct({ ...input, initialStock: Number(initialStock) });
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["products"] });
      if (product) onDone();
      else { setName(""); setDescription(""); setPrice(""); setInitialStock("0"); } // clear the form for the next product
    },
  });

  function onSubmit(event: FormEvent) {
    event.preventDefault();
    save.mutate();
  }

  return (
    <form onSubmit={onSubmit} className="panel space-y-3 p-5">
      <h2 className="text-lg">{product ? `Edit product #${product.id}` : "New product"}</h2>
      <div>
        <label htmlFor="p-name" className="label">Name</label>
        <input id="p-name" className="input" required maxLength={200} value={name} onChange={(e) => setName(e.target.value)} />
      </div>
      <div>
        <label htmlFor="p-desc" className="label">Description</label>
        <textarea id="p-desc" className="input min-h-20" value={description} onChange={(e) => setDescription(e.target.value)} />
      </div>
      <div className="grid grid-cols-2 gap-3">
        <div>
          <label htmlFor="p-price" className="label">Price</label>
          <input id="p-price" type="number" required min="0" step="0.01" className="input" value={price} onChange={(e) => setPrice(e.target.value)} />
        </div>
        <div>
          <label htmlFor="p-cat" className="label">Category</label>
          <select id="p-cat" className="input" required value={categoryId} onChange={(e) => setCategoryId(e.target.value)}>
            <option value="" disabled>Choose…</option>
            {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        </div>
      </div>
      {!product && (
        <div>
          <label htmlFor="p-stock" className="label">Starting stock</label>
          <input id="p-stock" type="number" min="0" required className="input w-32" value={initialStock} onChange={(e) => setInitialStock(e.target.value)} />
        </div>
      )}
      <ErrorMessage error={save.error} />
      {save.isSuccess && !product && <SuccessMessage>Product created.</SuccessMessage>}
      <div className="flex gap-2">
        <button type="submit" className="btn btn-primary" disabled={save.isPending}>{save.isPending ? "Saving…" : product ? "Save changes" : "Create product"}</button>
        {product && <button type="button" className="btn btn-secondary" onClick={onDone}>Cancel</button>}
      </div>
    </form>
  );
}

function CategoryForm({ categories }: { categories: Category[] }) {
  const queryClient = useQueryClient();
  const [name, setName] = useState("");
  const [parentId, setParentId] = useState("");

  const create = useMutation({
    mutationFn: () => adminApi.createCategory(name.trim(), parentId ? Number(parentId) : null),
    onSuccess: () => {
      setName(""); setParentId("");
      return queryClient.invalidateQueries({ queryKey: ["categories"] });
    },
  });

  return (
    <form onSubmit={(e) => { e.preventDefault(); create.mutate(); }} className="panel h-fit space-y-3 p-5">
      <h2 className="text-lg">New category</h2>
      <div>
        <label htmlFor="c-name" className="label">Name</label>
        <input id="c-name" className="input" required maxLength={100} value={name} onChange={(e) => setName(e.target.value)} />
      </div>
      <div>
        <label htmlFor="c-parent" className="label">Parent category (optional)</label>
        <select id="c-parent" className="input" value={parentId} onChange={(e) => setParentId(e.target.value)}>
          <option value="">None (top level)</option>
          {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
        </select>
      </div>
      <ErrorMessage error={create.error} />
      {create.isSuccess && <SuccessMessage>Category created.</SuccessMessage>}
      <button type="submit" className="btn btn-primary" disabled={create.isPending}>Create category</button>
    </form>
  );
}

/*
  Stock changes are relative: type 20 to add twenty units, or -5 to remove five.
  Relative changes stay correct even if a customer's order reserves stock at the same moment.
*/
function StockControl({ productId }: { productId: number }) {
  const queryClient = useQueryClient();
  const [delta, setDelta] = useState("");
  const adjust = useMutation({
    mutationFn: () => adminApi.adjustStock(productId, Number(delta)),
    onSuccess: () => {
      setDelta("");
      return queryClient.invalidateQueries({ queryKey: ["stock"] }); // reload the Available and Reserved columns
    },
  });

  return (
    <form className="space-y-1" onSubmit={(e) => { e.preventDefault(); if (delta !== "" && Number(delta) !== 0) adjust.mutate(); }}>
      <div className="flex gap-2">
        <input aria-label={`Stock change for product ${productId}`} type="number" step="1" placeholder="+20 or -5"
               className="input w-28" value={delta} onChange={(e) => setDelta(e.target.value)} />
        <button type="submit" className="btn btn-secondary" disabled={adjust.isPending}>Apply</button>
      </div>
      {adjust.isSuccess && (
        <p className="text-xs text-ok">Now {adjust.data.availableQty} available, {adjust.data.reservedQty} reserved.</p>
      )}
      {adjust.error && <p className="text-xs text-bad">{adjust.error.message}</p>}
    </form>
  );
}

/*
  Available = units customers can still buy.
  Reserved  = units already taken by orders that have not shipped yet (unpaid or paid).
  Units on the shelf = available + reserved. Shipping an order removes its units from "reserved".
*/
function StockCells({ stock, loading }: { stock: StockResponse | undefined; loading: boolean }) {
  if (!stock) {
    return (
      <>
        <td className="num p-3 text-right text-muted">{loading ? "…" : "-"}</td>
        <td className="num p-3 text-right text-muted">{loading ? "…" : "-"}</td>
      </>
    );
  }
  // Colour is a hint, and the number is always shown too, so nothing depends on colour alone.
  const availableClass = stock.availableQty === 0 ? "font-semibold text-bad" : stock.availableQty <= 5 ? "font-semibold text-[#7a5b00]" : "";
  return (
    <>
      <td className={`num p-3 text-right ${availableClass}`}>
        {stock.availableQty}
        {stock.availableQty === 0 && <span className="block text-xs font-normal">Out of stock</span>}
        {stock.availableQty > 0 && stock.availableQty <= 5 && <span className="block text-xs font-normal">Low</span>}
      </td>
      <td className="num p-3 text-right">{stock.reservedQty}</td>
    </>
  );
}
