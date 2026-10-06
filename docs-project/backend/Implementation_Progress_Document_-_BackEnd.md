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

The frontend is scaffolded (Angular 22, Tailwind CSS v4, `@ngrx/signals`); the application itself and its tests are still to be implemented.

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

Flyway is configured explicitly for Spring Boot 4.x. Test configuration also uses baseline settings for fresh Testcontainers databases.

## 4. Domain Models

Domain aggregates are regular Lombok classes without JPA or Spring annotations.

Value objects:

* `Money` — amount + currency, default EUR
* `Email` — regex validation and lowercase normalisation
* `NIF` — Portuguese 9-digit validation with check digit

Aggregates:

* `Product` — reserve, release, activate, deactivate, confirmSale
* `Customer` — block, activate
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

Cart items use `orphanRemoval = true`. Order items do not use orphan removal because they are historical records.

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
* `/api/v1/payments`

POST /api/v1/payments processes a simulated payment; the order owner (or ADMIN) may call it.

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
* `iat`
* `exp`

Security behaviour:

* Stateless authentication
* BCrypt password hashing
* Refresh token rotation
* `customerId` ownership checks for customer-scoped routes
* Order ownership checks for ID/order-number routes
* ADMIN bypass for order ownership checks
* Customer-scoped routes (cart routes and `GET /orders/customer/{customerId}`) do not exempt ADMIN
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

Documented domain, service and mapper tests cover:

* ShopOrder state transitions and cancellation
* Product stock operations
* Cart behaviour and totals
* Customer state changes
* Payment behaviour
* Money, Email and NIF value objects
* Application service flows
* DTO/domain mappers

### Persistence Integration Tests

Five flow suites:

* ProductFlowIntegrationTest — 5 tests
* CustomerFlowIntegrationTest — 5 tests
* CartFlowIntegrationTest — 7 tests
* OrderFlowIntegrationTest — 9 tests
* PaymentFlowIntegrationTest — 4 tests

Total persistence integration tests: 30.

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

Messaging tests do not use `@Transactional` because the RabbitMQ consumer uses its own database session. Cleanup is performed explicitly.

### Report Tests

`ReportServiceTest` (5 tests) covers workbook structure and aggregation. `ReportControllerTest` (5 tests) covers authorisation and validates the returned XLSX content.

### Address-in-Checkout Tests

`OrderServiceTest.shouldThrowWhenAddressDoesNotBelongToCustomer`, `CartControllerTest.shouldRejectCheckoutWithUnknownAddress` (422) and `OrderFlowIntegrationTest.shouldPersistDeliveryAddressSnapshot`. `CartControllerTest.shouldCheckoutCart` now checks the delivery fields in the response.

### Payment Tests

`PaymentServiceTest` (5 tests): success with order moved to PAID, order not found, order not PENDING, payment method differing from the order, payment already existing. `PaymentControllerTest` (6 tests): no token (401), payment approved and order PAID, another customer's order (403), second payment (422), different method (422), missing order (404).

### Shipping and Notification Tests

`OrderServiceTest` (2 tests): `ORDER_SHIPPED` event saved with the tracking code, and rejection of shipping without a tracking code. `OrderControllerTest` (2 tests): shipping with a tracking code (200) and without one (422). `OrderEventConsumersEmailTest` (5 tests, plain Mockito): each consumer calls its email method, a failed send still marks the event as processed, and a duplicate event sends nothing.

### Full Suite

211 tests, all passing:

* Unit — 108: domain 53 (ShopOrder 9, Product 9, Cart 8, Money 8, Email 6, NIF 6, Payment 3, CartItem 2, Customer 2), application services 36 (Order 11, Product 6, Cart 6, Category 4, Customer 4, Payment 5), mappers 9, ReportServiceTest 5, OrderEventConsumersEmailTest 5
* Application context load — 1
* Persistence integration — 30
* Controller integration — 65: Order 16, Product 12, Customer 7, Cart 8, Category 6, Report 5, Auth 5, Payment 6
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

## 15. In Progress

Backend: nothing pending.

Frontend: Angular 22 project scaffolded with Tailwind CSS v4 and `@ngrx/signals`; screens, state stores, unit tests and Cypress integration tests are still to be implemented.

## 16. Known Issues / Open Decisions

No defects and no open decisions in the backend.