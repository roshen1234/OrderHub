# OrderHub Web

A React storefront and admin console for the OrderHub Spring Boot backend.

**Stack:** React 19, TypeScript, Vite, React Router, TanStack Query, Tailwind CSS v4, Vitest.

## Run it

You need Node.js 20 or newer, and the backend running on `http://localhost:8080`.

```bash
npm install
npm run dev        # http://localhost:5173
```

| Command | What it does |
|---|---|
| `npm run dev` | Development server with hot reload |
| `npm run build` | Type-checks, then builds the production files into `dist/` |
| `npm run preview` | Serves the production build locally |
| `npm test` | Runs the tests once |

Optional: copy `.env.example` to `.env` to change the currency or locale used to format prices.

## Talking to the backend (no CORS setup needed in development)

The browser only talks to the Vite server. Requests that start with `/api` are forwarded to
`http://localhost:8080` with the `/api` prefix removed (see `vite.config.ts`):

```
GET http://localhost:5173/api/products   ->   GET http://localhost:8080/products
```

For production, serve the built files and the API from the **same domain** (for example with
nginx forwarding `/api` to the backend). If they must live on different domains, you then have to
add CORS configuration to Spring Boot.

## What is in the app

| Area | Pages |
|---|---|
| Visitors | Product list with search, category, price filters, sorting and paging; product detail |
| Customers | Sign in and register, cart, checkout with address picker, address book, orders list, order detail with progress timeline, pay and cancel |
| Admins | Product and category management, stock adjustment, orders list with ship and deliver, order detail, payment simulator |

## Project structure

```
src/
  main.tsx              entry point: sets up the query cache, router and auth
  App.tsx               the list of routes (URL -> page)
  index.css             design tokens (colors, font) and shared button/input classes
  types.ts              TypeScript types that mirror the backend's JSON
  api/                  one small file per area, each function = one backend endpoint
    client.ts           fetch wrapper: adds the token, handles errors, refreshes expired tokens
  auth/                 sign-in state, token storage, protected routes
  components/           Navbar, order timeline, shared UI pieces
  pages/                one file per screen (pages/admin for the admin area)
  lib/                  formatting, JWT reading, countdown hook
  test/                 test helpers
```

## How sign-in works

1. `POST /auth/login` returns an access token (15 minutes) and a refresh token (7 days).
2. The **access token** lives in memory only. It is sent as `Authorization: Bearer ...`.
3. The **refresh token** is kept in `localStorage`, so a page reload restores the session.
4. When the server answers `401`, `api/client.ts` calls `/auth/refresh`, saves the **new** refresh
   token (the backend rotates them, each works once) and repeats the request. Requests that fail at
   the same time share one refresh, and browser tabs take turns, so a token is never spent twice.
5. Hiding admin links is only convenience. The backend checks the role on every request.

**Trade-off:** `localStorage` can be read by malicious scripts if the site ever has an XSS bug.
The more secure design is for the backend to set the refresh token as an `HttpOnly` cookie.
That needs a small change in Spring Boot (`Set-Cookie` on login and refresh, `@CookieValue` on
refresh and logout), plus `credentials: "include"` here.

## Paying with the mock gateway

`POST /payments` creates a payment that stays `PENDING`. A real gateway would then call your
webhook. With the mock gateway, an admin plays the gateway:

1. Customer clicks **Pay now** on the order page. In development the page shows the payment reference.
2. An admin opens **Admin > Payment simulator**, pastes the reference and sends "Payment succeeded".
3. The order page polls every 3 seconds and switches to **Paid**.

The simulator only works while the backend has `orderhub.payment.gateway=mock`.

## Assumptions about the backend

These come from the API as built in the project. If a screen shows an error, compare the field names in
`src/types.ts` and the paths in `src/api/` with your controllers and DTOs.

- `GET /products` accepts `name, categoryId, minPrice, maxPrice, page, size, sort` and returns
  `{ content, page, size, totalElements, totalPages }`.
- `GET /categories` returns an array of `{ id, name, parentId }`.
- `GET /admin/orders` returns the same page shape; the `status` filter is used only if the backend supports it.
- Validation errors come back as `{ field: message }`, other errors as `{ error: message }`.
- Dates are `LocalDateTime` strings without a time zone, read here as local time.

## Ideas for next steps

- Move the refresh token to an `HttpOnly` cookie (see above).
- Add product images (an image URL on the product, or file upload).
- Add `react-hook-form` and `zod` if the forms grow more complex.
- Add ESLint and a CI step that runs `npm run build` and `npm test`.
- Containerize: build with Node, serve `dist/` with nginx, and forward `/api` to the backend.
