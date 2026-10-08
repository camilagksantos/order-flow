# order-flow — Implementation Progress Document (Backend)

## 1. Current Status

The backend implementation covers the main application, persistence, messaging, security and reporting flows.

Implemented areas:

* Database migrations
* Application configuration
* Domain models and value objects
* Domain exceptions
* JPA entities
* Input and output ports
* Application services
* Spring Data JPA repositories
* Persistence mappers and adapters
* DTOs and application mappers
* REST controllers
* JWT authentication and authorization
* RabbitMQ configuration, outbox scheduler and consumers
* Email adapter
* Centralised exception handling
* OpenAPI configuration
* Sales report generation
* Unit and backend integration tests

## 2. Database Migrations

Implemented migrations:

* `V1__create_schema.sql`
* `V2__rename_address_is_default_column.sql`
* `V3__add_customer_email_to_shop_order.sql`
* `V4__seed_roles.sql`
* `V5__add_delivery_address_to_shop_order.sql`

Current database decisions:

* Singular table names
* `shop_order` instead of `order` because `order` is reserved in MySQL
* String UUID primary keys for cart, cart item, shop order, order item, payment, outbox event and processed event
* Auto-increment `BIGINT` primary keys for role, user, category, product, customer and address
* Portugal localisation using NIF, district, postal code and default country PT
* Cart has no expiration
* V4 seeds `ADMIN` and `CUSTOMER` roles using `INSERT IGNORE`
* V5 adds nullable `delivery_*` columns to `shop_order` for the delivery address snapshot; nullable because existing orders have no address

## 3. Application Configuration

Main configuration is maintained in `application.yaml`.

Key settings:

* `spring.jpa.open-in-view: false`
* `spring.jpa.hibernate.ddl-auto: validate`
* Hibernate dialect is auto-detected by Hibernate 7
* MySQL, RabbitMQ and MailHog are configured for local Docker development
* `app.mail.from` is externalised
* JWT access token expiration: 15 minutes
* JWT refresh token expiration: 7 days
* `app.cookie.secure: false` for development; the code defaults to `true` when the property is missing, and production sets `APP_COOKIE_SECURE=true`
* `app.jwt.secret` holds a development value; production sets `APP_JWT_SECRET`
* `app.cors.allowed-origin: http://localhost:4200` for development; the code defaults to the same value when the property is missing, and production sets `APP_CORS_ALLOWED_ORIGIN`

Flyway is configured explicitly for Spring Boot 4.x. Test configuration also uses baseline settings for fresh Testcontainers databases.

## 4. Domain Models

Domain aggregates are regular Lombok classes without JPA or Spring annotations.

Value objects:

* `Money` — amount + currency, default EUR
* `Email` — regex validation and lowercase normalisation
* `NIF` — Portuguese 9-digit validation with check digit

Aggregates:

* `Product` — reserve, release, activate, deactivate, confirmSale
* `Customer` — block, activate, addAddress, findAddress, updateAddress, makeAddressDefault, removeAddress
* `Cart` — addItem, removeItem, convert, newCart
* `ShopOrder` — pay, startPreparing, ship, deliver, cancel, fromCart
* `Payment` — approve, decline

Supporting entities:

* Category
* Address
* CartItem
* OrderItem
* OutboxEvent

Authentication domain:

* User
* Role

Enums currently include:

* `ProductStatus`: ACTIVE, INACTIVE, DISCONTINUED
* `CustomerStatus`: ACTIVE, INACTIVE, BLOCKED
* `CartStatus`: ACTIVE, CONVERTED, ABANDONED
* `OrderStatus`: PENDING, PAID, PREPARING, SHIPPED, DELIVERED, CANCELLED
* `PaymentMethod`: CREDIT_CARD, MBWAY, MULTIBANCO
* `PaymentStatus`: PENDING, PROCESSING, APPROVED, DECLINED
* `OutboxEventStatus`: PENDING, SENT, FAILED

Domain events:

* OrderCreatedEvent
* OrderPaidEvent
* OrderCancelledEvent
* OrderShippedEvent
* OrderStatusChangedEvent

## 5. Ports and Application Services

### Output Ports

Located in `application/port/output/`.

* `ProductRepositoryPort`
* `CategoryRepositoryPort`
* `CustomerRepositoryPort`
* `CartRepositoryPort`
* `OrderRepositoryPort`
* `PaymentRepositoryPort`
* `OutboxEventRepositoryPort`
* `ProcessedEventRepositoryPort`
* `EmailNotificationPort`
* `UserRepositoryPort`
* `RoleRepositoryPort`

### Input Ports

Located in `application/port/input/`.

* CreateProductUseCase
* FindProductUseCase
* UpdateProductUseCase
* DeleteProductUseCase
* CreateCategoryUseCase
* FindCategoryUseCase
* RegisterCustomerUseCase
* FindCustomerUseCase
* AddAddressUseCase
* UpdateAddressUseCase
* SetDefaultAddressUseCase
* RemoveAddressUseCase
* AddToCartUseCase
* RemoveFromCartUseCase
* FindCartUseCase
* CheckoutUseCase
* FindOrderUseCase
* UpdateOrderStatusUseCase
* CancelOrderUseCase
* ProcessPaymentUseCase
* GenerateSalesReportUseCase

### Application Services

* `CategoryService`
* `ProductService`
* `CustomerService`
* `CartService`
* `OrderService`
* `PaymentService`
* `ReportService`

Key current behaviour:

* `OrderService.checkout()` is transactional.
* `CartService.addToCart()` creates a cart when none exists.
* `CartService` builds price/name/SKU snapshots server-side from the product.
* `ShopOrder.fromCart()` receives payment method explicitly.
* `CustomerService.registerCustomer()` creates the User and assigns the CUSTOMER role before saving the Customer.
* `CustomerService` address actions (`addAddress`, `updateAddress`, `setDefaultAddress`, `removeAddress`) are transactional: each loads the customer, applies the rule of the `Customer` aggregate and saves it. The address is looked up only in the customer's own list, so an unknown id and another customer's id both return 404. The created address returned by `addAddress` is the one with the highest id.
* `ProductService.updateProduct()` loads the existing product and preserves SKU, status and reserved quantity.
* `ReportService` generates the sales workbook using Apache POI.
* `PaymentService.processPayment()` is transactional: it validates the order (PENDING, same payment method, no previous payment), takes the amount from the order, approves the payment with a simulated transaction id, calls `order.pay()` and saves an `ORDER_PAID` outbox event.
* `OrderService.updateOrderStatus(orderId, status, trackingCode)` saves an `ORDER_PAID` event when the status changes to PAID and an `ORDER_SHIPPED` event when it changes to SHIPPED; shipping requires a non-blank tracking code (422 otherwise).
* `OrderService.cancelOrder()` saves an `ORDER_CANCELLED` event, so reserved stock is released through the consumer.

## 6. Persistence

### JPA Entities

Located in `infrastructure/persistence/entity/`.

All normal relationships are LAZY except `UserEntity.roles`, which is EAGER for Spring Security.

Entities include:

* RoleEntity
* UserEntity
* CategoryEntity
* ProductEntity
* CustomerEntity
* AddressEntity
* CartEntity
* CartItemEntity
* ShopOrderEntity
* OrderItemEntity
* PaymentEntity
* OutboxEventEntity
* ProcessedEventEntity

Cart items and customer addresses use `orphanRemoval = true`. Order items do not use orphan removal because they are historical records. `CustomerEntity.addresses` is ordered by id (`@OrderBy("id ASC")`).

### Repositories

Located in `infrastructure/persistence/repository/`.

Repositories include dedicated fetch queries where required to avoid `LazyInitializationException` when adapters are called without an open Hibernate session:

* `CartJpaRepository.findByIdWithItems()`
* `CartJpaRepository.findByCustomerIdAndStatus()` with items
* `ProductJpaRepository.findByIdWithCategory()`
* `CustomerJpaRepository.findByIdWithAddresses()`
* `ShopOrderJpaRepository.findByIdWithItems()`
* `ShopOrderJpaRepository.findByCreatedAtBetween()` with items

### Persistence Mappers

Located in `infrastructure/persistence/mapper/`.

MapStruct mappers include:

* RolePersistenceMapper
* UserPersistenceMapper
* CategoryPersistenceMapper
* ProductPersistenceMapper
* AddressPersistenceMapper
* CustomerPersistenceMapper
* CartItemPersistenceMapper
* CartPersistenceMapper
* OrderItemPersistenceMapper
* ShopOrderPersistenceMapper
* PaymentPersistenceMapper
* OutboxEventPersistenceMapper

Type conversions use default methods. Relationship references use shallow entity IDs or parent-level `@AfterMapping` methods as required by the relationship structure.

### Persistence Adapters

Located in `infrastructure/adapter/output/persistence/`.

* CategoryJpaAdapter
* ProductJpaAdapter
* CustomerJpaAdapter
* CartJpaAdapter
* OrderJpaAdapter
* PaymentJpaAdapter
* OutboxEventJpaAdapter
* ProcessedEventJpaAdapter
* UserJpaAdapter
* RoleJpaAdapter

The four adapters with relevant LAZY graphs reload entities through dedicated `JOIN FETCH` queries after `findById()` and `save()` when necessary.

## 7. DTOs and REST API

### Request DTOs

* `CreateCategoryRequest`
* `CreateProductRequest`
* `UpdateProductRequest`
* `RegisterCustomerRequest`
* `CreateAddressRequest`
* `UpdateAddressRequest`
* `AddToCartRequest`
* `CheckoutRequest`
* `UpdateOrderStatusRequest`
* `CancelOrderRequest`
* `ProcessPaymentRequest`
* `LoginRequest`

Important request details:

* `UpdateProductRequest` does not expose SKU because SKU is immutable.
* `AddToCartRequest` contains only `productId` and `quantity`; price/name/SKU are populated server-side.
* `CheckoutRequest` contains `idempotencyKey`, `addressId` and `paymentMethod`. `addressId` must belong to the authenticated customer and is snapshotted onto the order.
* `RegisterCustomerRequest` contains the password used to create the associated User.
* `CreateAddressRequest` and `UpdateAddressRequest` carry the same text fields (street, number, optional complement, neighborhood, city, district and postal code in the format `XXXX-XXX`); neither carries the country or the default flag, because the country is always `PT` and the default is decided by the domain.
* `UpdateOrderStatusRequest` contains `status` and an optional `trackingCode`, which is required when the status is `SHIPPED`.

### Response DTOs

* CategoryResponse
* ProductResponse
* AddressResponse
* CustomerResponse
* CartItemResponse
* CartResponse
* OrderItemResponse
* OrderResponse
* PaymentResponse
* TokenResponse
* ErrorResponse

`OrderResponse` includes `cancelReason` and the `delivery*` address fields together with status, items, amounts, payment method, tracking code and timestamps.

`TokenResponse` contains `accessToken`, `tokenType` and `expiresIn`. The refresh token is not part of the body: it is sent only in the `HttpOnly` cookie.

### REST Controllers

Located in `infrastructure/adapter/input/web/`.

* `CategoryController`
* `ProductController`
* `CustomerController`
* `CartController`
* `OrderController`
* `ReportController`
* `AuthController`
* `PaymentController`

Main routes:

* `/api/v1/categories`
* `/api/v1/products`
* `/api/v1/customers`
* `/api/v1/carts`
* `/api/v1/orders`
* `/api/v1/reports/sales`
* `/api/v1/auth/login`
* `/api/v1/auth/refresh`
* `/api/v1/auth/logout`
* `/api/v1/payments`

POST /api/v1/payments processes a simulated payment; the order owner (or ADMIN) may call it.

Address routes, owner only: `POST /api/v1/customers/{customerId}/addresses` (201), `PUT .../addresses/{addressId}` (200), `PATCH .../addresses/{addressId}/default` (200) and `DELETE .../addresses/{addressId}` (204). The addresses of a customer are returned by `GET /api/v1/customers/{id}`.

Public routes include registration, authentication, product reads, category reads and API documentation.

Customer routes require authentication and ownership of the requested customer/order resources.

Administrative routes include product management, category creation, order-status updates and report generation.

## 8. RabbitMQ and Messaging

### RabbitMQ Configuration

Located in `infrastructure/config/RabbitMQConfig.java`.

Exchanges:

* `orderflow.orders` — TopicExchange
* `orderflow.notifications` — FanoutExchange
* `orderflow.dlx` — FanoutExchange

Queues:

* `order.created.queue`
* `order.paid.queue`
* `order.shipped.queue`
* `order.cancelled.queue`
* `email.notification.queue`
* `orderflow.dead-letter.queue`

Key configuration:

* Jackson 3 compatible `JacksonJsonMessageConverter`
* Explicit `RabbitAdmin`
* `setDefaultRequeueRejected(false)`
* Explicit dead-letter queue binding

### Outbox Scheduler

`OutboxEventScheduler` runs every 5 seconds, reads `PENDING` outbox events and publishes the full `OutboxEvent` object.

Successful publication changes the event to `SENT`; failures are recorded as `FAILED`.

### Consumers

* `OrderCreatedConsumer` — reserves stock and sends the confirmation email
* `OrderPaidConsumer` — confirms sale and updates stock
* `OrderCancelledConsumer` — releases reserved stock and sends the cancellation email
* `OrderShippedConsumer` — loads the order, records the processed event and sends the shipped email

All consumers check `processed_event` before processing.

The email consumers send the email after saving the processed event, inside their own `try/catch`: a failed send is logged and does not fail the message, so it never reaches the dead-letter queue or repeats the stock change.

### Publisher Status

`EventPublisherPort` and `RabbitMQEventPublisher` were removed as dead code: nothing called them, since `OrderService` persists the `OutboxEvent` directly. Publication happens only through `OutboxEventScheduler`.

## 9. Email Adapter

Located in `infrastructure/adapter/output/email/MailEmailAdapter.java`.

Uses `JavaMailSender` and sends through MailHog during development.

Supported notifications:

* Order confirmation
* Order shipped
* Order cancelled

Each notification is called by the matching consumer (`OrderCreatedConsumer`, `OrderShippedConsumer`, `OrderCancelledConsumer`) through `EmailNotificationPort`.

The sender address is externalised through `app.mail.from`.

`ShopOrder.customerEmail` is stored as a snapshot so the email adapter does not need an additional customer repository lookup.

## 10. Exception Handling

Located in `infrastructure/config/handler/GlobalExceptionHandler.java`.

Current mappings:

* `ResourceNotFoundException` → 404
* `BusinessRuleException` → 422
* `MethodArgumentNotValidException` → 400
* `DataIntegrityViolationException` → 409
* Authentication failures (`AuthenticationException`) → 401
* `JwtException` (invalid or expired token) → 401
* `AccessDeniedException` → 403
* Generic `Exception` → 500

The authentication exception handler uses Spring Security's `AuthenticationException` type.

## 11. Security

Located in `infrastructure/config/security/`.

Components:

* `JwtService`
* `UserDetailsServiceImpl`
* `JwtAuthenticationFilter`
* `SecurityConfig`
* `SecurityUtils`
* `AuthController`

JWT claims:

* `sub` — user email
* `roles` — granted authorities
* `customerId` — present for users with a Customer record
* `type` — `access` or `refresh`
* `iat`
* `exp`

Security behaviour:

* Stateless authentication
* BCrypt password hashing
* Refresh token reissued on every refresh; the previous token is not revoked
* Refresh token delivered only in a cookie named `refreshToken` with `HttpOnly`, `SameSite=Strict` and `Path=/api/v1/auth`; the `Secure` attribute comes from `app.cookie.secure`
* Only `access` tokens authenticate API requests (`JwtAuthenticationFilter`); only `refresh` tokens are accepted by `/api/v1/auth/refresh`
* `POST /api/v1/auth/logout` is public and clears the cookie
* `customerId` ownership checks for customer-scoped routes
* Order ownership checks for ID/order-number routes
* ADMIN bypass for order ownership checks
* `GET /api/v1/customers/{id}` is allowed to the owner and to ADMIN; another customer gets 403 (`SecurityUtils.requireCustomerOrAdminAccess`)
* Customer-scoped routes (cart routes, address routes and `GET /orders/customer/{customerId}`) do not exempt ADMIN
* CORS origin read from `app.cors.allowed-origin`, with credentials allowed
* `401` for unauthenticated requests
* `403` for authenticated users without permission

## 12. Reports

`ReportService` generates an `.xlsx` sales report with Apache POI.

Sheets:

* `Orders` — order number, customer email, status, creation time, subtotal, shipping cost, discount, total and payment method
* `Summary` — count and total amount by `OrderStatus` plus a `TOTAL` row

`OrderRepositoryPort.findByCreatedAtBetween()` is implemented with `JOIN FETCH` for order items. The requested end date is inclusive for the calendar day.

## 13. Test Coverage

### Unit Tests

Documented domain, service, mapper and consumer tests cover:

* ShopOrder state transitions and cancellation
* Product stock operations
* Cart behaviour and totals
* Customer state changes and address rules
* Payment behaviour
* Money, Email and NIF value objects
* Application service flows
* DTO/domain mappers
* Email calls from the order event consumers

### Persistence Integration Tests

Five flow suites:

* ProductFlowIntegrationTest — 5 tests
* CustomerFlowIntegrationTest — 10 tests
* CartFlowIntegrationTest — 7 tests
* OrderFlowIntegrationTest — 9 tests
* PaymentFlowIntegrationTest — 4 tests

Total persistence integration tests: 35.

Testcontainers provide real MySQL and RabbitMQ infrastructure. The containers use the singleton pattern and are started manually from static fields so JUnit does not stop them between test classes.

### Controller Integration Tests

Uses `@AutoConfigureMockMvc` and real JWTs generated through `JwtService`. Tokens for customer-scoped routes carry the `customerId` claim, as `AuthController` issues at login.

Controller suites:

* CategoryControllerTest
* ProductControllerTest
* CustomerControllerTest
* CartControllerTest
* OrderControllerTest
* ReportControllerTest
* AuthControllerTest
* PaymentControllerTest

The tests also cover customer ownership (cart and per-customer order routes) and order ownership by ID and order number, including the admin exemption.

### Messaging Integration Tests

`MessagingFlowIntegrationTest` contains 7 tests covering:

* Order-created stock reservation
* Order-paid stock confirmation
* Order-cancelled stock release
* Order-shipped processed-event registration
* Duplicate-event idempotency
* Outbox publication and status update
* Dead-letter routing for permanently failing messages

Messaging tests do not use `@Transactional` because the RabbitMQ consumer uses its own database session. Cleanup is performed explicitly. In the test context there is no SMTP server, so email sends can fail; the failure is only logged and does not affect the assertions.

### Report Tests

`ReportServiceTest` (5 tests) covers workbook structure and aggregation. `ReportControllerTest` (5 tests) covers authorisation and validates the returned XLSX content.

### Address-in-Checkout Tests

`OrderServiceTest.shouldThrowWhenAddressDoesNotBelongToCustomer`, `CartControllerTest.shouldRejectCheckoutWithUnknownAddress` (422) and `OrderFlowIntegrationTest.shouldPersistDeliveryAddressSnapshot`. `CartControllerTest.shouldCheckoutCart` also checks the delivery fields in the response.

### Payment Tests

`PaymentServiceTest` (5 tests): success with order moved to PAID and outbox event saved, order not found, order not PENDING, payment method differing from the order, payment already existing. `PaymentControllerTest` (6 tests): no token (401), payment approved and order PAID, another customer's order (403), second payment (422), different method (422), missing order (404).

### Shipping and Notification Tests

`OrderServiceTest` (2 tests): `ORDER_SHIPPED` event saved with the tracking code, and rejection of shipping without a tracking code. `OrderControllerTest` (2 tests): shipping with a tracking code (200) and without one (422). `OrderEventConsumersEmailTest` (5 tests, plain Mockito): each consumer calls its email method, a failed send still marks the event as processed, and a duplicate event sends nothing.

### Authentication Cookie Tests

`AuthControllerTest` (9 tests, 4 of them new): login sets the `HttpOnly` cookie scoped to `/api/v1/auth` and does not return the refresh token in the body, refresh through the cookie, refresh with an invalid token (401), refresh without the cookie (401), an access token sent to `/refresh` (401), a refresh token sent as `Bearer` (401), logout clearing the cookie (204), plus the existing blank-password (400) and wrong-password (401) cases.

### Customer and Address Tests

`CustomerTest` (12 tests): block and activate, first address becomes the default, a second one does not, adding to a null list, editing (country unchanged), editing an unknown address, changing the default, removing a non-default address, removing the default (the first remaining is promoted), refusing to remove the last address, and removing an unknown address. `CustomerServiceTest` (11 tests, 7 new): add (returns the created address with its id and the default flag), unknown customer, edit, edit unknown address, set default, remove, and refuse to remove the last one. `CustomerFlowIntegrationTest` (10 tests, 5 new): saved addresses with only the first as default, adding to an existing customer, the row of a removed address is deleted, the new default is persisted and edited fields are persisted. `CustomerControllerTest` (19 tests, 10 new for addresses and 2 new for the read rule): owner reads, another customer gets 403, ADMIN reads any customer and gets 404 for a missing one; creating, editing, changing the default and deleting addresses, including the invalid postal code (400), another customer (403), unknown address (404), deleting the last address (422) and no token (401).

### CORS Tests

`CorsIntegrationTest` (3 tests): the preflight of `http://localhost:4200` is allowed with credentials, the preflight of an unknown origin is rejected with 403, and a real request from the frontend origin receives the CORS headers.

### Full Suite

252 tests, all passing:

* Unit — 125: domain 63 (ShopOrder 9, Product 9, Cart 8, Money 8, Email 6, NIF 6, Payment 3, CartItem 2, Customer 12), application services 43 (Order 11, Product 6, Cart 6, Category 4, Customer 11, Payment 5), mappers 9, ReportServiceTest 5, OrderEventConsumersEmailTest 5
* Application context load — 1
* Persistence integration — 35
* Controller integration — 81: Order 16, Product 12, Customer 19, Cart 8, Category 6, Report 5, Auth 9, Payment 6
* CORS integration — 3
* Messaging integration — 7

## 14. Decisions and Final-State Notes

The following implementation decisions are considered part of the current design:

* Domain aggregates use Lombok classes; DTOs and value objects use records where appropriate.
* MapStruct replaced ModelMapper.
* Port packages use `input` and `output`.
* Category is under `domain/category/`.
* Cart never expires.
* Delivery address is snapshotted on the order at checkout (`delivery*` fields), validated against the authenticated customer's addresses.
* No stock movement audit table is maintained.
* No payment idempotency key is used because there is no real payment gateway integration.
* `OrderItem` keeps historical records without orphan removal.
* Cart items use orphan removal.
* `OrderItem.subtotal` is a persisted field.
* `customerEmail` is stored as an order snapshot for email delivery.
* Jackson 3 / Spring AMQP 4 configuration is used.
* `spring-boot-starter-flyway` is explicitly included for Spring Boot 4.x.
* Payment is simulated: approved on request, with no gateway and no payment idempotency key (one payment per order is enforced instead).
* Every order change with a side effect (created, paid, shipped, cancelled) writes an outbox event in the same transaction; the consumers apply the stock change and send the email.
* Shipping requires a tracking code, sent in `UpdateOrderStatusRequest`.
* Emails are sent after the processed event is saved and their failures are only logged; a failed email is not retried.
* The refresh token is delivered only in an `HttpOnly`, `SameSite=Strict` cookie scoped to `/api/v1/auth`; the access token is returned in the response body, and `/refresh` issues a new one from the cookie.
* Access and refresh tokens carry a `type` claim and cannot be used in place of each other. Before this change a refresh token worked as a `Bearer` access token.
* Refresh tokens are reissued on every refresh but are not revoked on the server: a stolen refresh token stays valid until it expires or the secret changes. Revocation would need a refresh-token table.
* `SameSite=Strict` requires the frontend and the API to be served from the same site.
* `app.jwt.secret`, `app.cookie.secure` and `app.cors.allowed-origin` in `application.yaml` are development values; production sets `APP_JWT_SECRET`, `APP_COOKIE_SECURE=true` and `APP_CORS_ALLOWED_ORIGIN`.
* `GET /api/v1/customers/{id}` is allowed to the owner and to ADMIN. Before this change any authenticated user could read any customer record.
* Customer addresses are managed through four routes (create, edit, change default, delete), owner only. The first address becomes the default, a customer always keeps at least one address, deleting the default promotes the first remaining one, and the country is always `PT` and not editable.
* Customer addresses use `orphanRemoval` and are ordered by id, so deleting removes the row and "the first remaining" is stable. Orders keep their own copy of the delivery address, so deleting an address does not affect them.
* The CORS origin is a single configurable origin, and the default blocks everything except `http://localhost:4200`.

## 15. In Progress

Backend: nothing pending.

## 16. Known Issues / Open Decisions

No defects and no open decisions in the backend.