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
- Inter and Cormorant Garamond, self-hosted through the `@fontsource-variable/inter` and `@fontsource-variable/cormorant-garamond` packages
- RxJS 7.8
- Vitest 5 with jsdom, run through the Angular builder `@angular/build:unit-test` (`ng test`)
- Cypress for end-to-end tests (not installed yet)
- Node 22 managed with `nvm`, Angular CLI 22.2

The project does not use a UI component library. All styling is done with Tailwind utility classes in the templates.

## 3. Project Structure

Structure under `frontend/src/app/`, grouped by subject. Items marked as planned do not exist yet:

```text
app/
├── app.ts, app.html, app.config.ts, app.routes.ts
├── models/         ← data types, one file per type, grouped by subject
│   ├── auth/       ← login-request, token-response, auth-user, access-token-payload
│   ├── customer/   ← customer, address, customer-status
│   ├── shared/     ← load-status; later the types used by several subjects (Money, OrderStatus)
│   ├── toast/      ← toast, toast-kind
│   └── cart/, catalog/, order/   ← planned
├── core/           ← used by the whole app or by the layout
│   ├── auth/           ← auth-api, auth.store
│   ├── guard/          ← auth-guard, guest-guard, not-found-guard
│   ├── interceptor/    ← auth-interceptor
│   ├── customer/       ← customer-api, customer.store
│   ├── search/         ← search.store
│   ├── notification/   ← toaster, toast-container
│   ├── layout/
│   │   ├── brand.ts
│   │   ├── shell/
│   │   ├── header/     ← header, header-brand, header-menu-button, header-search-button, header-search-panel, header-cart
│   │   └── menu/       ← menu, menu-greeting, menu-links, menu-session (menu-filters planned)
│   ├── catalog/        ← planned: product-api, category-api, the product list and the filter state
│   └── cart/           ← planned: cart-api, cart.store
├── features/       ← one lazy-loaded page per area
│   ├── auth/login/
│   ├── catalog/product-list/    ← banner, highlights, grid and card planned as separate components
│   ├── cart/cart-page/
│   ├── checkout/checkout-page/  ← components for the address and the payment method planned
│   └── orders/order-list/       ← order card with expandable detail planned
└── shared/         ← reusable, no business rules (planned: button, alert, spinner, money pipe)
```

`app.routes.ts` points directly to each page, loaded on demand with `loadComponent`, because each area has a single screen. A routes file per area is only created when an area gets several routes (the admin area of phase 2, as `features/admin/`). Empty folders are not created in advance. Unit tests sit next to the file they test (`login.spec.ts`), and Cypress tests live in `frontend/cypress/`.

### Placement rule

A file used by one area lives inside that area. A file used by two or more areas, or by the layout, lives in `core/`, grouped by subject. `core/cart/` holds the cart because the header shows the item count, the catalog adds items and the checkout reads the cart. `core/customer/` holds the customer because the menu greeting and the checkout both use it, and `core/catalog/` will hold the product list because the menu filters and the product page both use it. When the admin area of phase 2 needs `order-api`, it moves from its feature to `core/`.

Data types do not follow this rule: they all live in `app/models/<subject>/`, one file per type, like the DTO package of the backend. A type that only the frontend uses (such as `AuthUser`) goes in the same folder as the others of its subject. A type used by several subjects goes in `models/shared/`.

Guards and the interceptor are the exception to grouping by subject: they are registered in the routes and in the application configuration, so they are grouped by type in `core/guard/` and `core/interceptor/`. The API service and the store of a subject stay together in its folder (for example `core/auth/`).

Each component has its own folder named after it, with the `.ts`, the `.html` and the `.spec.ts`. In `core/layout/`, a component that groups parts (`header`, `menu`) has the folder of its parts inside its own folder.

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
- Each component has a single responsibility. A component that groups parts only places them and holds the state they share: `Header` receives `menuOpen` and emits `menuToggle`, and `Menu` receives `open` and emits `closed`. Each part reads only what it needs and handles its own loading and failure.
- Templates are separate `.html` files.
- Tailwind classes are written directly in the templates.
- A component `.css` file is created only when a style cannot be expressed with Tailwind classes (for example a custom animation). It is not generated by default.
- The interface text is in Portuguese from Portugal (pt-PT). Code, names and commit messages are in English.
- Global visual settings are defined in `src/styles.css` through the Tailwind `@theme` block. The page layout structure lives in the `shell` template.

### Signal-based APIs

Everything is built on the signal-based APIs of Angular 22:

- Component state uses `signal()` and `computed()`, and inputs use `input()`.
- Templates use the control flow blocks `@if` and `@for`.
- Forms use Signal Forms (`@angular/forms/signals`), which are stable in Angular 22. Reactive Forms are not used.
- Reads (GET) that depend on signals, such as the catalog, the product detail and the order list, use `httpResource`.
- Actions (login, add to cart, checkout, payment, cancel) use `HttpClient` inside the stores, converted with `firstValueFrom`. Components await the store methods.
- Components use `OnPush`, which is the default for new components in Angular 22.

### Visual design

The theme lives in the `@theme` block of `src/styles.css`.

- Colors: the scales `wine` (400 to 900), `sand` (50 to 500), `cappuccino` (300 to 600) and `chocolate` (600 to 800), plus the roles `page` (`#FAF6F0`, page background), `surface` and `white` (`#FFFFFF`, cards, forms, inputs and modals), `soft` (`#F2E9DD`, alternate areas), `line` (`#E5D6C4`) and `line-strong` (`#D3BDA4`) for borders, `ink` (`#423027`) and `ink-soft` (`#785F50`) for text, `danger` (`#B42332`) and `success` (`#467653`).
- The main color is wine 600 (`#702630`), for primary buttons and links, always with white text. Hover uses wine 700. As a guide, about 60% of the interface is cream and sand, 25% cappuccino and other earthy tones, and 15% wine.
- White is an official color of the identity. A white card on the cream page has a contrast of only 1.08:1, so every white card has a `line` border and a soft shadow.
- Cappuccino is decorative only (icons, lines, badges). It is never a text color nor a background for white text.
- Secondary text uses `ink-soft` on the page and on white, and `chocolate-600` on sand 200 and darker areas.
- Errors and successes always carry an icon and a text, never only a color. The error color is a carmine, close to the wine, so the icon and the message are what tell it apart from a brand highlight.
- Fonts: Inter (variable) for the text and Cormorant Garamond (variable, with italic) for `h1` to `h3` and the brand name. They are self-hosted and loaded through the `styles` list of `angular.json`.
- Shadows `soft`, `lift` and `overlay` have two layers tinted with dark wine at low opacity. Radii are `control` (0.625 rem) and `card` (1 rem). The easing is `ease-soft`, and transitions last between 250 and 350 ms.
- Enter and leave animations use `animate.enter` and `animate.leave` with CSS classes (`animate-toast-in`, `animate-toast-out`, `animate-panel-in`, `animate-panel-out`, `animate-fade-in`, `animate-fade-out`), not `@angular/animations`, which is deprecated. A global rule shortens animations and transitions for users who ask the system to reduce motion.
- Only a light theme exists. The page uses `lang="pt-PT"` and the title "Velora". The brand name is a constant in `core/layout/brand.ts`.

## 5. Routes and Guards

| Route | Screen | Access |
| --- | --- | --- |
| `/` | redirects to `/products` | public |
| `/products` | catalog with filters and expandable product detail | public |
| `/login` | login with a registration box | guests only |
| `/cart` | cart | logged in |
| `/checkout` | address, payment method, confirmation | logged in |
| `/orders` | own orders with expandable detail, cancel and pay | logged in |
| any other path | redirects to `/products` with a notice | public |

Each route has a title (Produtos, Entrar, Carrinho, Finalizar compra, Meus pedidos). Adding the suffix " · Velora" to the tab title and scrolling to the top on every page change are planned for step 2. Phase 2 adds `/admin/...` routes behind a role guard for `ADMIN`. All pages are lazy-loaded. Today the five pages are empty components.

Three functional guards live in `core/guard/`:

- `authGuard` lets a logged-in user through and sends a guest to `/login?returnUrl=<requested url>`.
- `guestGuard` lets a guest through and sends a logged-in user to `/products`. It protects `/login`.
- `notFoundGuard`, on the catch-all route, shows the notice "Página não encontrada" and redirects to `/products`.

An unknown route, and a screen that the user is not allowed to open (403), both lead to `/products` with the notice "Página não encontrada", so nobody finds out that a restricted page exists. A guest on a screen that needs login is sent to the login instead, with no notice. Form errors (a wrong password, for example) keep the user on the screen, with a toast and the invalid field marked. In the catalog and in the orders list only one item is expanded at a time.

## 6. State Management

State uses `signalStore` from `@ngrx/signals`.

- `AuthStore` (global, `core/auth/auth.store.ts`): holds `accessToken` and `user`, exposes `isAuthenticated`, `isAdmin` and `customerId`, and has the methods `login`, `logout`, `restoreSession`, `refresh` and `clearSession`.
- `CustomerStore` (global, `core/customer/customer.store.ts`): holds the logged-in `customer` (with the addresses) and a `status` of type `LoadStatus` (`idle`, `loading`, `loaded` or `error`), and exposes `firstName`. It observes the `customerId` of `AuthStore`: when it appears the store loads the customer, and when it disappears the store clears. A late response from a user who already left is ignored. It is created at startup, so the name is ready when the menu opens.
- `SearchStore` (global, `core/search/search.store.ts`): holds the applied search term, with `hasTerm`, `setTerm` and `clear`. The text being typed lives in the search panel.
- `Toaster` (`core/notification/toaster.ts`): a service that holds the list of notices in a signal.
- `CartStore` (global, planned in `core/cart/`): the cart and its item count, which the header displays.
- Catalog data (planned in `core/catalog/`): the product list, loaded with `httpResource`, and the filter state, read by the menu filters and by the product page.
- Feature stores (local, inside the feature): for state used by one area only.
- Whether the menu is open is state of `Shell`, and whether the search panel is open is state of `Header`.

## 7. Authentication and Session

The backend side of this flow is described in backend Context sections 6.11 and 6.13.

- The access token (15 minutes) is kept only in memory, inside `AuthStore`. It is never written to `localStorage` or `sessionStorage`.
- The refresh token (7 days) is never visible to the application. The backend sets it in an `HttpOnly`, `SameSite=Strict` cookie scoped to `/api/v1/auth`, and the browser manages it.
- Calls to `/api/v1/auth/*` are sent with credentials so the browser includes the cookie. Other calls do not need it.
- `roles` and `customerId` come from the payload of the access token, because `TokenResponse` only carries `accessToken`, `tokenType` and `expiresIn`. The payload is decoded on the client with the `jwt-decode` library, without verifying the signature: it only drives what is shown, and the backend remains the authority on every request.
- The first name shown in the menu comes from `GET /api/v1/customers/{id}`, loaded by `CustomerStore` after the login or after the session is restored.
- In development the frontend (`localhost:4200`) and the API (`localhost:8080`) count as the same site, which `SameSite=Strict` requires. In production both must be served from the same site.

### Interceptor

`authInterceptor` (`core/interceptor/`) adds `Authorization: Bearer <accessToken>` only to calls to the backend API (`<apiUrl>/api/`). It skips the `/api/v1/auth/*` routes, other domains and calls made without a token.

When a call made while logged in returns 401, the interceptor asks the store to refresh the token and repeats the call once with the new token. If the refresh fails, the store clears the session and the error is returned to the caller. If the repeated call also returns 401, that error is returned. A 401 from an auth route, or while nobody is logged in, never triggers a refresh, which avoids an infinite loop. The interceptor does not navigate: sending the user to the login is the job of the guards.

Simultaneous 401s share a single refresh request: while one refresh is in progress, `AuthStore.refresh()` returns the same promise.

### Session at startup and logout

- At startup, `provideAppInitializer` calls `AuthStore.restoreSession()` before the app appears. If the cookie is valid the session is restored after a page reload; otherwise the user is treated as logged out and no error is raised. A guest opening the app for the first time causes an expected 401 from the refresh route, which shows in the browser console. A second initializer creates `CustomerStore`.
- A loading spinner shown while the app waits for this refresh is planned for `index.html` (step 2 of the build order).
- `AuthStore.logout()` calls `POST /api/v1/auth/logout` and clears the in-memory session even if that call fails; the cookie then expires on its own.
- Signing out from the menu ends the session, closes the menu, shows the notice "Sessão terminada" and goes to `/products`, so nobody stays on a protected screen.

## 8. API Layer and Environments

Each API service lives next to its area, following the placement rule in section 3, is declared with `@Service()`, and builds its URLs from `environment.apiUrl`. There is one service per backend controller and no generic wrapper around `HttpClient`.

Environments are generated with `ng generate environments`:

- `src/environments/environment.ts` — base file, `production: true`
- `src/environments/environment.development.ts` — replaces the base file in the `development` build through `fileReplacements` in `angular.json`
- Both currently define `apiUrl: 'http://localhost:8080'`

### Backend contract used by the frontend

- Authentication: `POST /api/v1/auth/login`, `/refresh`, `/logout`
- Registration: `POST /api/v1/customers`
- Customer: `GET /api/v1/customers/{id}` (own record, with the addresses), `POST /api/v1/customers/{customerId}/addresses`, `PUT .../addresses/{addressId}`, `PATCH .../addresses/{addressId}/default`, `DELETE .../addresses/{addressId}`
- Catalog: `GET /api/v1/products` and `GET /api/v1/categories` (public)
- Cart: `GET /api/v1/carts/customer/{customerId}`, `POST .../items`, `DELETE .../items/{itemId}`, `POST .../checkout`
- Orders: `GET /api/v1/orders/{id}`, `/number/{orderNumber}`, `/customer/{customerId}`, `POST /{id}/cancel`
- Payment: `POST /api/v1/payments`

Checkout flow:

1. The client generates the idempotency key (a UUID) and calls the checkout route with `idempotencyKey`, `addressId` and `paymentMethod`.
2. The client calls `POST /api/v1/payments` with the returned `orderId` and the same method. The amount is never sent: the backend takes it from the order, and rejects a method that differs from the one chosen at checkout.
3. The client opens `/orders`, with the new order expanded, which shows its status.

Error responses are shown according to the backend mapping: 400 validation, 401 session, 403 forbidden, 404 not found, 409 conflict and 422 business rule.

The search, the filters and the highlights of the catalog are done on the client over the full product list. The `ProductController`, the `CategoryResponse` and `Money` still have to be reviewed to confirm that the list is not paginated and to know the price format.

## 9. Testing Strategy

- Unit and component tests use Vitest through `ng test`, with the spec file next to the source file.
- Dependencies are replaced by fakes provided through `TestBed` (for example a fake `AuthApi` in the store tests, fake stores in the component tests, and `HttpTestingController` for HTTP calls).
- Routes are tested by navigating with `RouterTestingHarness`, and timers with Vitest fake timers.
- Each component tests only its own responsibility; behavior of a part is not repeated in the test of the component that groups it.
- Coverage uses `ng test --coverage`, which requires the `@vitest/coverage-v8` package. The minimum of 70% is enforced through `coverageThresholds` in the `test` options of `angular.json`. Neither is configured yet.
- End-to-end tests use Cypress against the running backend and cover the main journeys: register, login, add to cart, checkout with payment, and order status.

## 10. Layout and Interaction

### Shell

`Shell` (`core/layout/shell/`) is the only component rendered by `App`. It places the toolbar, the menu, the `<main>` area with the router outlet (centered, at most 72 rem wide, with space for the fixed toolbar) and the toast container, and holds whether the menu is open.

### Toolbar

`Header` is a fixed bar 4 rem high, white with transparency and blur, with a `line` border and a soft shadow. Its parts are `HeaderMenuButton` (the menu icon, which turns into an X while the menu is open), `HeaderBrand` (the name "Velora" in the serif font, linking to `/products`), `HeaderSearchButton` and `HeaderCart` (a link to `/cart`; the item count badge comes with the cart step). The buttons have a touch area of at least 44 px. The search panel is placed outside the bar because the blur of the bar would otherwise change how a fixed element inside it is positioned.

### Search

The text typed in `HeaderSearchPanel` does not filter anything by itself: while the user types, a line "Pesquisar «text»" appears under the field. `Enter` or a click on that line applies the term (`SearchStore.setTerm`) and goes to `/products`. `Esc`, the X button or the search button clear the term and close the panel, and the panel also clears the term when it is destroyed. The filtering itself is done in the catalog, over the product list.

### Menu

The menu always opens. It is made of `MenuGreeting`, `MenuLinks`, `MenuSession` and, planned, `MenuFilters` (only on `/products`). It descends from the top over the page with a darkened backdrop, takes the full width on phones and 22 rem on the left on larger screens, gives the focus to its first item when it opens, and closes with a click on the backdrop, `Esc` or the choice of a page. The focus is not trapped inside it.

- `MenuGreeting` shows "Olá, visitante" to a guest, a placeholder bar while the name loads, "Olá, Ana" with the first name, and "Olá!" if loading fails or the account has no customer record (as an admin account).
- `MenuLinks` shows Produtos to everybody and Meus pedidos only to a logged-in user, with the current page highlighted and marked with `aria-current`.
- `MenuSession` shows Entrar to a guest and Sair to a logged-in user.
- The planned filters are the category, a price range, only available products and the order by price or name, all applied on the client.

### Notifications

`Toaster` has `success`, `error` and `info`. It keeps at most three notices, the newest first. Success and info last 4 seconds and an error lasts 7; hovering or focusing a notice pauses its countdown, and each one has a close button. `ToastContainer` shows them at the top right under the toolbar, and across the width on phones, with an icon, a text and an accent color. An error has `role="alert"` and the others `role="status"`.

## 11. Planned Screens

- Catalog (`/products`): a banner (a static image with a wine gradient, a title and a subtitle that enter softly, and a very slow zoom), a strip of five products picked at random on the client at each visit, and a grid of cards whose detail expands in place, one at a time. A product with `availableQuantity` 0 shows "Esgotado" and its add button is disabled. The stock fields are never shown. There are no promotions or coupons.
- Login (`/login`): the login form and a registration box over it, both with Signal Forms. The registration fields are name, e-mail, NIF (9 digits), an optional phone and a password of 8 to 100 characters. After the login, the user returns to the `returnUrl`.
- Cart (`/cart`).
- Checkout (`/checkout`): a summary of the items, the delivery address (choosing one, or creating a new one inside the screen), the payment method (a card with its brand and last four digits, MB Way with the phone, or Multibanco with nothing else) and the confirmation. An empty cart sends the user back to the cart.
- Orders (`/orders`): a list with the expandable detail, the status and the tracking code, the cancellation, and a Pay button for orders in `PENDING`, which covers a payment that failed after the order was created.