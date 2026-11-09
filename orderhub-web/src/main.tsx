import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import "./index.css";
import App from "./App";
import { AuthProvider } from "./auth/AuthContext";
import { ApiError } from "./api/client";

/*
  TanStack Query (React Query) fetches, caches and refreshes server data.
  Instead of useEffect + useState + loading flags in every component, you write:
      const { data, isPending, error } = useQuery({ queryKey: [...], queryFn: ... });
  The queryKey identifies the data in the cache. Calling queryClient.invalidateQueries({ queryKey })
  after a change (for example after adding to the cart) makes it reload.
*/
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000, // data counts as fresh for 30 seconds, so going back to a page is instant
      refetchOnWindowFocus: false,
      // Retry network hiccups once, but never retry 4xx answers: a 404 or 409 will not change.
      retry: (failureCount, error) =>
        !(error instanceof ApiError && error.status >= 400 && error.status < 500) && failureCount < 1,
    },
  },
});

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    {/* Order matters: AuthProvider uses the query client, and everything uses the router. */}
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AuthProvider>
          <App />
        </AuthProvider>
      </BrowserRouter>
    </QueryClientProvider>
  </StrictMode>,
);
