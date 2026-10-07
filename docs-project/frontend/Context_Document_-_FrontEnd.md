# order-flow — Frontend Context Document

## 1. Overview

The frontend is an Angular single-page application that consumes the order-flow REST API. The API contract, security rules and error mapping are described in the backend Context Document (`docs-project/backend/Context_Document_-_BackEnd.md`), mainly sections 6.9 (checkout), 6.11 (security), 6.12 (exception handling), 6.13 (CORS) and 6.21 (payments).

The project is part of a portfolio, and its focus is Angular and Tailwind CSS.

Delivery is split in two phases:

- Phase 1 — customer storefront: authentication, catalog, cart, checkout, payment and orders
- Phase 2 — admin area: product management, order status updates with tracking code, sales report

## 2. Technology Stack

- Angular 22.2 with standalone components (no NgModules, no SSR)
- TypeScript 6.0
- Tailwind CSS 4.3, configured through `@tailwindcss/postcss` in `.postcssrc.json` and `@import "tailwindcss";` in `src/styles.css`
- `@ngrx/signals` 22 (`signalStore`) for state
- RxJS 7.8
- Vitest 5 with jsdom, run through the Angular builder `@angular/build:unit-test` (`ng test`)
- Cypress for end-to-end tests (not installed yet)
- Node 22 managed with `nvm`, Angular CLI 22.2

The project does not use a UI component library. All styling is done with Tailwind utility classes in the templates.

## 3. Project Structure

Planned structure under `frontend/src/app/`:

```text
app/
├── app.ts, app.html, app.css, app.config.ts, app.routes.ts
├── core/
│   ├── api/        ← one file per backend controller
│   │   ├── auth-api.ts
│   │   ├── product-api.ts
│   │   ├── category-api.ts
│   │   ├── customer-api.ts
│   │   ├── cart-api.ts
│   │   ├── order-api.ts
│   │   └── payment-api.ts
│   ├── auth/
│   │   ├── jwt.ts              ← reads roles and customerId from the token payload
│   │   ├── auth-interceptor.ts ← adds the Bearer header and renews the token on 401
│   │   ├── auth-guard.ts       ← blocks routes that require login
│   │   └── guest-guard.ts      ← keeps logged-in users out of login and register
│   └── layout/
│       ├── shell.ts            ← header + router outlet
│       └── header.ts
├── stores/         ← state used by two or more areas
│   ├── auth-store.ts
│   └── cart-store.ts
├── features/       ← one lazy-loaded area per folder
│   ├── auth/       ← login, register
│   ├── catalog/    ← product-list, product-detail, components/product-card, catalog-store
│   ├── cart/       ← cart-page
│   ├── checkout/   ← checkout-page, components (address, payment method)
│   └── orders/     ← order-list, order-detail, components/order-status-badge
└── shared/         ← reusable, no business rules
    ├── ui/         ← button, alert, spinner
    ├── pipes/      ← money (EUR formatting)
    └── models/     ← types used by several areas (Money, Address, OrderStatus)
```

Each feature has its own `<feature>.routes.ts`, and `app.routes.ts` only points to them. The admin area of phase 2 will be added as `features/admin/`. Empty folders are not created in advance. Unit tests sit next to the file they test (`login.spec.ts`), and Cypress tests live in `frontend/cypress/`.

### Dependency rule

- `shared` imports from nothing
- `core` imports only from `shared`
- `stores` import from `core` and `shared`
- `features` import from `stores`, `core` and `shared`, and never from another feature

HTTP calls live in `core/api/` so that a store and a screen can use the same API service without one feature depending on another. A store used by a single area lives inside that area (for example `catalog-store.ts`); a store used by two or more areas lives in `stores/`.

## 4. Naming and Component Conventions

- Angular 20 and later no longer add `.component` or `.service` suffixes. Files are named by what they are (`login.ts`, `cart-store.ts`) and classes follow (`Login`, `CartStore`), as in the generated `app.ts`.
- One concept per file.
- Templates are separate `.html` files.
- Tailwind classes are written directly in the templates.

## 5. Routes and Guards

| Route | Screen | Access |
| --- | --- | --- |
| `/` | redirects to `/products` | public |
| `/products` | catalog with filters | public |
| `/products/:id` | product detail and add to cart | public |
| `/login`, `/register` | authentication | guests only |
| `/cart` | cart | logged in |
| `/checkout` | address, payment method, confirmation | logged in |
| `/orders` | own orders | logged in |
| `/orders/:id` | order detail, status, cancel | logged in |

Phase 2 adds `/admin/...` routes behind a role guard for `ADMIN`. All feature routes are lazy-loaded.

## 6. State Management

State uses `signalStore` from `@ngrx/signals`.

- `AuthStore` (global): session state, login, logout, refresh at application start, roles and customer id.
- `CartStore` (global): the cart and its item count, which the header displays.
- Feature stores (local): for example the catalog filters and pagination.

## 7. Authentication and Session

The backend side of this flow is described in backend Context sections 6.11 and 6.13.

- The access token (15 minutes) is kept only in memory, inside `AuthStore`. It is never written to `localStorage` or `sessionStorage`.
- The refresh token (7 days) is never visible to the application. The backend sets it in an `HttpOnly`, `SameSite=Strict` cookie scoped to `/api/v1/auth`, and the browser manages it.
- Calls to `/api/v1/auth/*` are sent with credentials so the browser includes the cookie. Other calls do not need it.
- An interceptor adds `Authorization: Bearer <accessToken>` to API calls. On a 401 it requests a new access token once through `/api/v1/auth/refresh` and repeats the call; if that fails, the session ends and the user goes to the login.
- On application start, before the first route is activated, the application calls `/api/v1/auth/refresh`. If the cookie is valid the session is restored after a page reload; otherwise the user is treated as logged out.
- Logout calls `POST /api/v1/auth/logout` and clears the in-memory state.
- `roles` and `customerId` come from the payload of the access token, because `TokenResponse` only carries `accessToken`, `tokenType` and `expiresIn`. The payload is decoded on the client without verifying the signature: it only drives what is shown, and the backend remains the authority on every request.
- In development the frontend (`localhost:4200`) and the API (`localhost:8080`) count as the same site, which `SameSite=Strict` requires. In production both must be served from the same site.

## 8. API Layer and Environments

`core/api/` contains one service per backend controller, and each builds its URLs from `environment.apiUrl`. There is no generic wrapper around `HttpClient`.

Environments are generated with `ng generate environments`:

- `src/environments/environment.ts` — base file, `production: true`
- `src/environments/environment.development.ts` — replaces the base file in the `development` build through `fileReplacements` in `angular.json`
- Both currently define `apiUrl: 'http://localhost:8080'`

### Backend contract used by the frontend

- Authentication: `POST /api/v1/auth/login`, `/refresh`, `/logout`
- Registration: `POST /api/v1/customers`
- Catalog: `GET /api/v1/products` and `GET /api/v1/categories` (public)
- Cart: `GET /api/v1/carts/customer/{customerId}`, `POST .../items`, `DELETE .../items/{itemId}`, `POST .../checkout`
- Orders: `GET /api/v1/orders/{id}`, `/number/{orderNumber}`, `/customer/{customerId}`, `POST /{id}/cancel`
- Payment: `POST /api/v1/payments`

Checkout flow:

1. The client generates the idempotency key (a UUID) and calls the checkout route with `idempotencyKey`, `addressId` and `paymentMethod`.
2. The client calls `POST /api/v1/payments` with the returned `orderId` and the same method. The amount is never sent: the backend takes it from the order, and rejects a method that differs from the one chosen at checkout.
3. The client opens `/orders/:id`, which shows the order status.

Error responses are shown according to the backend mapping: 400 validation, 401 session, 403 forbidden, 404 not found, 409 conflict and 422 business rule.

To be confirmed when the corresponding backend controllers are reviewed: product filtering, search and pagination parameters, and the routes for listing and creating customer addresses.

## 9. Testing Strategy

- Unit and component tests use Vitest through `ng test`, with the spec file next to the source file.
- Coverage uses `ng test --coverage`, which requires the `@vitest/coverage-v8` package. The minimum of 70% is enforced through `coverageThresholds` in the `test` options of `angular.json`. Neither is configured yet.
- End-to-end tests use Cypress against the running backend and cover the main journeys: register, login, add to cart, checkout with payment, and order status.