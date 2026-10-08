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
- `jwt-decode` to read the access token payload
- RxJS 7.8
- Vitest 5 with jsdom, run through the Angular builder `@angular/build:unit-test` (`ng test`)
- Cypress for end-to-end tests (not installed yet)
- Node 22 managed with `nvm`, Angular CLI 22.2

The project does not use a UI component library. All styling is done with Tailwind utility classes in the templates.

## 3. Project Structure

Planned structure under `frontend/src/app/`, grouped by subject:

```text
app/
├── app.ts, app.html, app.css, app.config.ts, app.routes.ts
├── models/         ← data types, one file per type, grouped by subject
│   ├── auth/       ← login-request, token-response, auth-user, access-token-payload
│   ├── cart/
│   ├── catalog/
│   ├── order/
│   └── shared/     ← types used by several subjects (Money, Address, OrderStatus)
├── core/           ← used by the whole app or by the header
│   ├── auth/
│   │   ├── auth-api.ts, auth.store.ts
│   │   ├── interceptor/ ← auth-interceptor
│   │   └── guard/       ← auth-guard, guest-guard
│   ├── cart/       ← cart-api, cart.store
│   ├── customer/   ← customer-api
│   └── layout/     ← shell, header
├── features/       ← one lazy-loaded area per folder
│   ├── auth/       ← login, register
│   ├── catalog/    ← product-list, product-detail, components/product-card, product-api, category-api, catalog.store
│   ├── cart/       ← cart-page
│   ├── checkout/   ← checkout-page, components (address, payment method), payment-api
│   └── orders/     ← order-list, order-detail, components/order-status-badge, order-api
└── shared/         ← reusable, no business rules
    ├── components/ ← button, alert, spinner
    └── pipes/      ← money (EUR formatting)
```

Each feature has its own `<feature>.routes.ts`, and `app.routes.ts` only points to them. The admin area of phase 2 will be added as `features/admin/`. Empty folders are not created in advance. Unit tests sit next to the file they test (`login.spec.ts`), and Cypress tests live in `frontend/cypress/`.

### Placement rule

A file used by one area lives inside that area. A file used by two or more areas, or by the header, lives in `core/`, grouped by subject. `core/cart/` holds the cart because the header shows the item count, the catalog adds items and the checkout reads the cart. `customer-api` is in `core/` because registration and the checkout addresses both use it. When the admin area of phase 2 needs `product-api` and `order-api`, they move from their features to `core/`.

Data types do not follow this rule: they all live in `app/models/<subject>/`, one file per type, like the DTO package of the backend. A type that only the frontend uses (such as `AuthUser`) goes in the same folder as the others of its subject. A type used by several subjects goes in `models/shared/`.

Inside `core/auth/`, the API service and the store stay at the top level, and the interceptor and the guards each have their own subfolder.

### Dependency rule

- `models` imports from nothing
- `shared` imports from nothing
- `core` imports only from `models` and `shared`
- `features` import from `core`, `models` and `shared`, and never from another feature

## 4. Naming and Component Conventions

- Angular 20 and later no longer add `.component` or `.service` suffixes. Files keep the names the IDE generator creates: services and components without a suffix (`auth-api.ts`, `login.ts`), stores with `.store` (`auth.store.ts`, class `AuthStore`), and specs with `.spec` next to the file they test.
- Services are declared with `@Service()`, the Angular 22 decorator that provides a root singleton without extra configuration.
- Data types are `type` aliases, one per file, without a suffix, in `models/<subject>/` (for example `login-request.ts`).
- One concept per file.
- Templates are separate `.html` files.
- Tailwind classes are written directly in the templates.
- A component `.css` file is created only when a style cannot be expressed with Tailwind classes (for example a custom animation). It is not generated by default.
- Global visual settings (palette, font, base styles) are defined in `src/styles.css`. The page layout structure lives in the `shell` template.

### Signal-based APIs

Everything is built on the signal-based APIs of Angular 22:

- Component state uses `signal()` and `computed()`, and inputs use `input()`.
- Templates use the control flow blocks `@if` and `@for`.
- Forms use Signal Forms (`@angular/forms/signals`), which are stable in Angular 22. Reactive Forms are not used.
- Reads (GET) that depend on signals, such as the catalog, the product detail and the order list, use `httpResource`.
- Actions (login, add to cart, checkout, payment, cancel) use `HttpClient` inside the stores, converted with `firstValueFrom`. Components await the store methods.
- Components use `OnPush`, which is the default for new components in Angular 22.

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

Two functional guards exist in `core/auth/guard/`, ready to be attached when the routes are created:

- `authGuard` lets a logged-in user through and sends a guest to `/login?returnUrl=<requested url>`.
- `guestGuard` lets a guest through and sends a logged-in user to `/products`.

## 6. State Management

State uses `signalStore` from `@ngrx/signals`.

- `AuthStore` (global, `core/auth/auth.store.ts`): holds `accessToken` and `user`, exposes `isAuthenticated`, `isAdmin` and `customerId`, and has the methods `login`, `logout`, `restoreSession`, `refresh` and `clearSession`.
- `CartStore` (global, in `core/cart/`): the cart and its item count, which the header displays.
- Feature stores (local, inside the feature): for example the catalog filters and pagination.

## 7. Authentication and Session

The backend side of this flow is described in backend Context sections 6.11 and 6.13.

- The access token (15 minutes) is kept only in memory, inside `AuthStore`. It is never written to `localStorage` or `sessionStorage`.
- The refresh token (7 days) is never visible to the application. The backend sets it in an `HttpOnly`, `SameSite=Strict` cookie scoped to `/api/v1/auth`, and the browser manages it.
- Calls to `/api/v1/auth/*` are sent with credentials so the browser includes the cookie. Other calls do not need it.
- `roles` and `customerId` come from the payload of the access token, because `TokenResponse` only carries `accessToken`, `tokenType` and `expiresIn`. The payload is decoded on the client with the `jwt-decode` library, without verifying the signature: it only drives what is shown, and the backend remains the authority on every request.
- In development the frontend (`localhost:4200`) and the API (`localhost:8080`) count as the same site, which `SameSite=Strict` requires. In production both must be served from the same site.

### Interceptor

`authInterceptor` (`core/auth/interceptor/`) adds `Authorization: Bearer <accessToken>` only to calls to the backend API (`<apiUrl>/api/`). It skips the `/api/v1/auth/*` routes, other domains and calls made without a token.

When a call made while logged in returns 401, the interceptor asks the store to refresh the token and repeats the call once with the new token. If the refresh fails, the store clears the session and the error is returned to the caller. If the repeated call also returns 401, that error is returned. A 401 from an auth route, or while nobody is logged in, never triggers a refresh, which avoids an infinite loop. The interceptor does not navigate: sending the user to the login is the job of the guards.

Simultaneous 401s share a single refresh request: while one refresh is in progress, `AuthStore.refresh()` returns the same promise.

### Session at startup and logout

- At startup, `provideAppInitializer` calls `AuthStore.restoreSession()` before the app appears. If the cookie is valid the session is restored after a page reload; otherwise the user is treated as logged out and no error is raised. A guest opening the app for the first time causes an expected 401 from the refresh route, which shows in the browser console.
- A loading spinner shown while the app waits for this refresh is planned for `index.html`, together with the theme (step 2 of the build order).
- `AuthStore.logout()` calls `POST /api/v1/auth/logout` and clears the in-memory session even if that call fails; the cookie then expires on its own.

## 8. API Layer and Environments

Each API service lives next to its area, following the placement rule in section 3, is declared with `@Service()`, and builds its URLs from `environment.apiUrl`. There is one service per backend controller and no generic wrapper around `HttpClient`.

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
- Dependencies are replaced by fakes provided through `TestBed` (for example a fake `AuthApi` in the store tests, and `HttpTestingController` for HTTP calls).
- Coverage uses `ng test --coverage`, which requires the `@vitest/coverage-v8` package. The minimum of 70% is enforced through `coverageThresholds` in the `test` options of `angular.json`. Neither is configured yet.
- End-to-end tests use Cypress against the running backend and cover the main journeys: register, login, add to cart, checkout with payment, and order status.