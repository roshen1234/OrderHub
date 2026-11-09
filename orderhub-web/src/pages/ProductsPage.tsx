import { useState, type FormEvent } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { keepPreviousData, useQuery } from "@tanstack/react-query";
import { catalogApi } from "../api/catalog";
import { EmptyState, ErrorMessage, Loading, Money, PageHeader, Pagination } from "../components/ui";
import type { Product } from "../types";

const SORT_OPTIONS = [
  { value: "id,desc", label: "Newest first" },
  { value: "price,asc", label: "Price: low to high" },
  { value: "price,desc", label: "Price: high to low" },
  { value: "name,asc", label: "Name: A to Z" },
];

/*
  The filters live in the URL (?name=phone&page=1), not in component state. That means:
  the back button works, a filtered view can be bookmarked or shared, and a reload keeps it.
  useSearchParams reads and writes that part of the URL.
*/
export function ProductsPage() {
  const [params, setParams] = useSearchParams();

  const query = {
    name: params.get("name") ?? "",
    categoryId: params.get("categoryId") ?? "",
    minPrice: params.get("minPrice") ?? "",
    maxPrice: params.get("maxPrice") ?? "",
    sort: params.get("sort") ?? "id,desc",
    page: Number(params.get("page") ?? 0),
    size: 12,
  };

  const categories = useQuery({ queryKey: ["categories"], queryFn: catalogApi.listCategories });

  const products = useQuery({
    queryKey: ["products", query], // a new query key means a new request, and each page is cached separately
    queryFn: () => catalogApi.listProducts(query),
    placeholderData: keepPreviousData, // keep showing the old page while the next one loads
  });

  /** Writes new filters into the URL, and goes back to the first page. */
  function applyFilters(values: Record<string, string>) {
    const next = new URLSearchParams();
    for (const [key, value] of Object.entries(values)) if (value) next.set(key, value);
    setParams(next);
  }

  function changePage(page: number) {
    const next = new URLSearchParams(params);
    next.set("page", String(page));
    setParams(next);
    window.scrollTo({ top: 0 });
  }

  return (
    <div>
      <PageHeader title="Shop" />

      <FilterBar
        key={params.toString() === "" ? "empty" : "set"} // remount to clear the fields when filters are reset
        initial={query}
        categories={categories.data ?? []}
        onApply={applyFilters}
      />

      <div className="mt-6">
        <ErrorMessage error={products.error} />
        {products.isPending ? (
          <Loading label="Loading products…" />
        ) : products.data && products.data.content.length === 0 ? (
          <EmptyState title="No products match these filters">
            Try a different search, or <button className="link" onClick={() => setParams({})}>clear the filters</button>.
          </EmptyState>
        ) : (
          <>
            <p className="mb-3 text-sm text-muted">
              {products.data?.totalElements} {products.data?.totalElements === 1 ? "product" : "products"}
            </p>
            <ul className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
              {products.data?.content.map((product) => (
                <li key={product.id}><ProductTile product={product} /></li>
              ))}
            </ul>
            <Pagination page={query.page} totalPages={products.data?.totalPages ?? 0} onChange={changePage} />
          </>
        )}
      </div>
    </div>
  );
}

function FilterBar({
  initial,
  categories,
  onApply,
}: {
  initial: { name: string; categoryId: string; minPrice: string; maxPrice: string; sort: string };
  categories: { id: number; name: string }[];
  onApply: (values: Record<string, string>) => void;
}) {
  // The fields are "controlled": React state is the single source of truth for what they show.
  const [name, setName] = useState(initial.name);
  const [categoryId, setCategoryId] = useState(initial.categoryId);
  const [minPrice, setMinPrice] = useState(initial.minPrice);
  const [maxPrice, setMaxPrice] = useState(initial.maxPrice);
  const [sort, setSort] = useState(initial.sort);

  function submit(event: FormEvent) {
    event.preventDefault();
    onApply({ name: name.trim(), categoryId, minPrice, maxPrice, sort: sort === "id,desc" ? "" : sort });
  }

  return (
    <form onSubmit={submit} className="panel grid gap-3 p-4 sm:grid-cols-2 lg:grid-cols-6">
      <div className="lg:col-span-2">
        <label htmlFor="q" className="label">Search</label>
        <input id="q" className="input" placeholder="Name contains…" value={name} onChange={(e) => setName(e.target.value)} />
      </div>
      <div>
        <label htmlFor="cat" className="label">Category</label>
        <select id="cat" className="input" value={categoryId} onChange={(e) => setCategoryId(e.target.value)}>
          <option value="">All categories</option>
          {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
        </select>
      </div>
      <div>
        <label htmlFor="min" className="label">Min price</label>
        <input id="min" type="number" min="0" step="0.01" className="input" value={minPrice} onChange={(e) => setMinPrice(e.target.value)} />
      </div>
      <div>
        <label htmlFor="max" className="label">Max price</label>
        <input id="max" type="number" min="0" step="0.01" className="input" value={maxPrice} onChange={(e) => setMaxPrice(e.target.value)} />
      </div>
      <div>
        <label htmlFor="sort" className="label">Sort by</label>
        <select id="sort" className="input" value={sort} onChange={(e) => setSort(e.target.value)}>
          {SORT_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
        </select>
      </div>
      <div className="sm:col-span-2 lg:col-span-6">
        <button type="submit" className="btn btn-primary">Apply filters</button>
      </div>
    </form>
  );
}

/*
  The API has no product images, so each tile gets a colored block with the product's initial.
  The color is derived from the category id, so products in one category look related.
*/
export function ProductPlate({ product, large = false }: { product: Product; large?: boolean }) {
  const hue = (product.categoryId * 47) % 360;
  return (
    <div
      aria-hidden="true"
      className={`flex items-center justify-center font-bold ${large ? "h-64 text-7xl" : "h-36 text-5xl"}`}
      style={{ background: `hsl(${hue} 40% 92%)`, color: `hsl(${hue} 45% 32%)`, fontStretch: "120%" }}
    >
      {product.name.charAt(0).toUpperCase()}
    </div>
  );
}

function ProductTile({ product }: { product: Product }) {
  return (
    <Link to={`/products/${product.id}`} className="panel block overflow-hidden transition-colors hover:border-ink">
      <ProductPlate product={product} />
      <div className="p-4">
        <p className="text-xs text-muted">{product.categoryName}</p>
        <h2 className="mt-0.5 line-clamp-2 text-base font-semibold" style={{ fontStretch: "100%" }}>{product.name}</h2>
        <p className="mt-2 font-semibold"><Money value={product.price} /></p>
      </div>
    </Link>
  );
}
