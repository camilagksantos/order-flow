# order-flow — Implementation Progress Document (Frontend)

## 1. Current Status

The Angular project is configured, the authentication core (step 1) is implemented, and step 2 is partly done: the theme, the notifications, the toolbar, the menu, the shell, the routes and the customer data exist. The five pages are still empty components. The architecture is described in `Context_Document_-_FrontEnd.md`.

## 2. Configuration Done

* Angular 22.2 project in `frontend/` with standalone components, routing and CSS, without SSR
* Tailwind CSS v4 configured: `.postcssrc.json` with `@tailwindcss/postcss` and `@import "tailwindcss";` in `src/styles.css`
* `@ngrx/signals` installed
* `jwt-decode` installed
* Fonts installed with `@fontsource-variable/inter` and `@fontsource-variable/cormorant-garamond`, loaded through the `styles` list of `angular.json`
* Theme in `src/styles.css`: the wine, sand, cappuccino and chocolate scales, the color roles, the fonts, the shadows, the radii, the easing, the enter and leave animations and the reduced motion rule
* `src/index.html` with `lang="pt-PT"` and the title "Velora"
* Environments generated with `ng generate environments`; `environment.ts` and `environment.development.ts` both define `apiUrl: 'http://localhost:8080'`, and the `development` build replaces the base file through `fileReplacements`
* `app.config.ts` registers the router, `HttpClient` with the auth interceptor, an app initializer that restores the session and another that creates `CustomerStore`
* Test runner: Vitest 5 with jsdom through `@angular/build:unit-test`
* The generated title test in `app.spec.ts` was removed because the template only contained the router outlet; the file now holds a single test that creates the app

## 3. Build Order

| Step | Content | Status |
| --- | --- | --- |
| 0 | Environments and scaffold cleanup | Done |
| 1 | Authentication core: `AuthApi`, `AuthStore`, interceptor with token renewal, session restore and logout, guards | Done |
| 2 | Theme, notifications, shell, toolbar, menu, routes and customer data; then the page title suffix, scroll to top, startup spinner, and the login screen with the registration box | In progress |
| 3 | Catalog: product data in `core/catalog/`, the menu filters, the banner, the highlights strip and the product grid | Not started |
| 4 | Cart | Not started |
| 5 | Checkout and payment | Not started |
| 6 | Orders | Not started |
| 7 | Cypress end-to-end tests | Not started |
| Phase 2 | Admin area: products, order status with tracking code, sales report | Not started |

Each step delivers a short description of each file, the code and the tests.

### Step 1 contents

* `models/auth/`: `LoginRequest`, `TokenResponse`, `AuthUser`, `AccessTokenPayload`, one file each
* `core/auth/auth-api.ts`: `login`, `refresh` and `logout`, all with credentials enabled
* `core/auth/auth.store.ts`: session state, `login`, `logout`, `restoreSession`, `refresh` (one shared request for simultaneous calls) and `clearSession`
* `core/interceptor/auth-interceptor.ts`: adds the bearer token and renews it on a 401
* `core/guard/auth-guard.ts` and `guest-guard.ts`
* `app.config.ts`: interceptor and `provideAppInitializer` calling `restoreSession`

### Step 2 contents done

* Theme and fonts, as listed in the configuration
* `models/toast/`: `Toast` and `ToastKind`; `core/notification/toaster.ts` and `toast-container`
* `core/search/search.store.ts`: the applied search term
* `core/layout/brand.ts` with the brand name
* `core/layout/header/`: `header` (the frame) and its parts `header-brand`, `header-menu-button`, `header-search-button`, `header-search-panel` and `header-cart`
* `core/layout/menu/`: `menu` (the panel) and its parts `menu-greeting`, `menu-links` and `menu-session`
* `core/layout/shell/shell.ts`, rendered alone by `App`
* `models/customer/`: `Customer`, `Address` and `CustomerStatus`; `models/shared/load-status.ts`
* `core/customer/customer-api.ts` and `customer.store.ts`
* `core/guard/not-found-guard.ts`
* `app.routes.ts` with the redirect of `/`, the five lazy-loaded pages with their titles and guards, and the catch-all route
* Empty pages: `features/auth/login`, `features/catalog/product-list`, `features/cart/cart-page`, `features/checkout/checkout-page` and `features/orders/order-list`

## 4. Test Coverage

133 unit tests, all passing, in 29 files:

* `App` — 1
* `AuthApi` — 3 (login, refresh, logout)
* `AuthStore` — 13 (initial state, customer login, admin login, failed login, refresh, failed refresh, shared refresh, new refresh after the previous one finished, clear session, logout, logout with a failing API, restore session, restore without a session)
* `authInterceptor` — 10 (header added, no token, auth routes, other domains, renewal and repeat, failed renewal, repeated 401, non-401 errors, 401 from an auth route, 401 without a logged-in user)
* Guards — 6 (`authGuard` 2, `guestGuard` 2 and `notFoundGuard` 2)
* `Toaster` — 8 and `ToastContainer` — 7
* `SearchStore` — 4
* `CustomerApi` — 1 and `CustomerStore` — 7
* `Header` — 8 and its parts: `HeaderBrand` 3, `HeaderMenuButton` 4, `HeaderSearchButton` 4, `HeaderCart` 3 and `HeaderSearchPanel` 9
* `Menu` — 8 and its parts: `MenuLinks` 5, `MenuSession` 6 and `MenuGreeting` 6
* `Shell` — 5
* The five empty pages — 5 (one "should create" each)
* Routes — 7 (root redirect, products for a guest, cart for a guest and for a logged-in user, login for a guest and for a logged-in user, unknown route with a notice)

Coverage tooling (`@vitest/coverage-v8`) and the 70% threshold are not configured yet. Cypress is not installed yet.

## 5. Decisions and Final-State Notes

* Test runner is Vitest, the default generated by Angular 22, instead of Jest
* Styling uses only Tailwind CSS, with no component library
* Phase 1 covers the customer storefront; the admin area is phase 2
* Everything is built on the signal-based APIs of Angular 22: signals, signal inputs, Signal Forms, `httpResource` for reads and the new control flow
* Services use `@Service()`; actions go through `HttpClient` in the stores, reads through `httpResource`
* The refresh token stays in an `HttpOnly` cookie managed by the browser; the access token lives only in memory (see `Context_Document_-_FrontEnd.md`, section 7)
* `roles` and `customerId` are read from the access token payload with the `jwt-decode` library
* Token renewal: only when a call made while logged in returns 401 and is not an auth route; one retry per call; simultaneous 401s share one refresh; a failed refresh clears the session and the interceptor does not navigate
* The app waits for the session restore before appearing; a guest opening the app causes an expected 401 in the console
* Logout clears the local session even if the backend call fails
* Guards: a guest is sent to `/login` with `returnUrl`, and a logged-in user opening the login is sent to `/products`
* The customer screens were reduced to five: the registration is a box over the login, and the product and order details expand inside their lists, one at a time
* An unknown route and a 403 lead to `/products` with the notice "Página não encontrada"; a guest on a protected screen goes to the login; form errors keep the user on the screen with a toast and the invalid field marked
* Visual identity: wine as the main color on a cream page with white surfaces, sand and cappuccino as support, chocolate for text; Inter for text and Cormorant Garamond for titles; light theme only; interface in pt-PT; brand name "Velora" (invented, no trademark check)
* White cards always have a border and a soft shadow; cappuccino is decorative only; errors and successes always carry an icon and a text
* Enter and leave animations use `animate.enter` and `animate.leave` with CSS classes, and a global rule reduces motion for users who ask for it
* The toolbar is fixed and has the menu, the brand, the search and the cart; the search applies only on `Enter` or on a click on the line "Pesquisar «…»", and closing it clears the term
* The menu always opens, descends from the top over the page, and each of its parts handles its own loading and failure; the first name comes from `CustomerStore`, which is created at startup
* Signing out from the menu ends the session, shows "Sessão terminada" and goes to `/products`
* Components have a single responsibility: the toolbar and the menu are frames that place small parts, and the toast notifications live in `core/notification/` because the service holds state
* Search, filters and the highlights strip of the catalog are done on the client over the full list of products; the highlights are five products picked at random at each visit
* Folder structure is grouped by subject; all data types live in `app/models/<subject>/`, one `type` per file and without a suffix; classes used by one area stay in that area, and those used by two or more areas or by the layout go to `core/`; guards and the interceptor are grouped by type in `core/guard/` and `core/interceptor/`
* File names follow the IDE generators: no suffix for services and components, `.store` for stores; each component has its own folder; when a file is generated, its full final path is checked
* Component `.css` files are created only when a style cannot be expressed with Tailwind classes
* The work goes one part at a time: decisions are settled before code, descriptions are short, and each file is created with the IDE generator and then adjusted
* Commit messages are written in English
* The editor may show errors such as `Cannot find name 'describe'` in new spec files until the TypeScript server is restarted; `ng test` is the reference
* The IDE quick fix that creates a missing function can paste a stub that throws `Function not implemented` into a spec; a search for that text finds it

## 6. In Progress

Step 2 continues with the page title suffix " · Velora" and the scroll to top on every page change, the startup spinner in `index.html`, and the login screen with the registration box.

## 7. Pending Decisions

1. Startup spinner in `index.html`, planned for step 2.
2. The login screen must read the `returnUrl` parameter and send the user back after the login (step 2).
3. Backend controllers still to be reviewed before the catalog step: `ProductController`, `CategoryResponse` and `Money`, to confirm that the product list is complete and not paginated, and the price format.
4. The menu filters (`MenuFilters`) and the product data in `core/catalog/` come with the catalog step.