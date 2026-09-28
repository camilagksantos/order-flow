# order-flow — Architecture Context Document

## 1. Final Architecture Plan

This project follows Hexagonal Architecture (Ports and Adapters) to maintain a clear separation between:

* Domain logic
* Application use cases
* Infrastructure
* External integrations

The core business logic resides in the domain layer and remains independent of Spring, JPA, RabbitMQ and web concerns. External systems communicate with the application through ports and adapters.

## 2. Domain Model

### Aggregates

* Product
* Customer
* Cart
* ShopOrder
* Payment

### Value Objects

* Money — amount + currency, default EUR
* Email — validated and lowercase enforced
* NIF — 9 digits with Portuguese check-digit validation

`Money`, `Email` and `NIF` remain Java records because they are value objects and do not expose behaviour methods that interfere with MapStruct.

### Supporting Entities

* Category
* Address
* CartItem
* OrderItem
* OutboxEvent
* User
* Role

### Domain Events

* OrderCreatedEvent
* OrderPaidEvent
* OrderCancelledEvent
* OrderShippedEvent
* OrderStatusChangedEvent

### Domain Exceptions

* DomainException — abstract base
* ResourceNotFoundException → HTTP 404

  * ProductNotFoundException
  * CustomerNotFoundException
  * CartNotFoundException
  * OrderNotFoundException
* BusinessRuleException → HTTP 422

  * InsufficientStockException
  * InvalidOrderStatusTransitionException

## 3. Order State Machine

Allowed transitions:

* PENDING → PAID, CANCELLED
* PAID → PREPARING
* PREPARING → SHIPPED, CANCELLED
* SHIPPED → DELIVERED

Transitions are validated inside the `ShopOrder` aggregate. Invalid transitions throw `InvalidOrderStatusTransitionException`.

## 4. Package Structure

```text
orderflow/
└── src/main/java/com/camilagksantos/orderflow/
    ├── domain/
    │   ├── auth/          ← User, Role
    │   ├── cart/          ← Cart, CartItem, CartStatus
    │   ├── category/      ← Category
    │   ├── customer/      ← Customer, Address, CustomerStatus
    │   ├── event/         ← DomainEvent, domain events, OutboxEvent
    │   ├── exception/     ← domain exceptions
    │   ├── order/         ← ShopOrder, OrderItem, OrderStatus, PaymentMethod
    │   ├── payment/       ← Payment, PaymentStatus
    │   ├── product/       ← Product, ProductStatus
    │   └── shared/        ← Money, Email, NIF
    ├── application/
    │   ├── port/
    │   │   ├── input/     ← use case interfaces
    │   │   └── output/    ← repository and external-service interfaces
    │   ├── service/       ← use case implementations
    │   ├── dto/           ← request / response models
    │   └── mapper/        ← domain ↔ DTO mappers
    └── infrastructure/
        ├── adapter/
        │   ├── input/
        │   │   ├── web/        ← REST controllers
        │   │   └── messaging/  ← RabbitMQ consumers
        │   └── output/
        │       ├── persistence/ ← JPA adapters
        │       ├── messaging/   ← messaging infrastructure
        │       └── email/       ← MailHog / SMTP adapter
        ├── persistence/
        │   ├── entity/      ← JPA entities
        │   ├── repository/  ← Spring Data JPA repositories
        │   └── mapper/      ← entity ↔ domain mappers
        └── config/          ← Spring beans, RabbitMQ, OpenAPI; handler/ (exception handling) and security/ subpackages
```

## 5. Environment Profiles

### dev

* MySQL 8 via Docker
* RabbitMQ via Docker
* MailHog via Docker
* Used for local development

The dev RabbitMQ keeps its state between restarts. Changing the type of an existing exchange (for example `orderflow.dlx`, changed from direct to fanout) makes the application fail at startup with `PRECONDITION_FAILED`, because RabbitMQ cannot redeclare an exchange with a different type. Delete the old exchange (`docker exec order-flow-rabbitmq rabbitmqadmin -u guest -p guest delete exchange name=<exchange>`) or recreate the volumes with `docker compose down -v`. Testcontainers are not affected because they start empty.

### test

* Testcontainers for MySQL and RabbitMQ
* Shared singleton containers for the test suite
* Used for integration tests
* Docker must be running before the integration tests start

### prod

* MySQL 8 external
* RabbitMQ external
* SMTP external
* Configuration through environment variables

## 6. Key Architectural Decisions

### 6.1 Domain Models and DTOs

Domain aggregates are regular Java classes using Lombok for boilerplate. They have no JPA or Spring annotations and expose domain behaviour through void mutation methods.

Examples:

* `Product` — reserve, release, activate, deactivate, confirmSale
* `Customer` — block, activate
* `Cart` — addItem, removeItem, convert
* `ShopOrder` — pay, startPreparing, ship, deliver, cancel
* `Payment` — approve, decline

DTOs are Java records because they are immutable transfer objects with no behaviour.

### 6.2 Persistence Mapping

JPA entities are regular Lombok classes in `infrastructure/persistence/entity/`.

Relationship loading:

* `LAZY` for normal relationships
* `UserEntity.roles` uses `EAGER` because the roles are required by Spring Security

Enums use `EnumType.STRING`.

Timestamps are managed through `@PrePersist` and `@PreUpdate`.

Primary keys:

* `Long` with `GenerationType.IDENTITY` for role, user, category, product, customer and address
* String UUIDs for cart, cart item, order, order item, payment, outbox event and processed event

Historical order items do not use `orphanRemoval`. Cart items do use `orphanRemoval` because they do not exist outside their cart.

### 6.3 MapStruct Strategy

Two mapper groups are used:

* `application/mapper/` — DTO ↔ domain
* `infrastructure/persistence/mapper/` — entity ↔ domain

MapStruct is used for compile-time, type-safe mapping.

Type conversion is implemented through default methods matched by method signature, without `@Named`, `qualifiedByName` or mapping expressions for the standard conversions.

Standard conversions include:

* `BigDecimal` ↔ `Money`
* `String` ↔ `Email`
* `String` ↔ `NIF`

Response DTOs use `Money`. Request DTOs use `BigDecimal` for price input.

The boolean field `isDefault` was renamed to `defaultAddress` to avoid Lombok/MapStruct getter conflicts.

### 6.4 Foreign-Key Reference Mapping

When the domain stores a raw ID but JPA expects an entity reference, persistence mappers build shallow entity references containing only the ID.

This pattern is used for:

* `Customer.userId` → `UserEntity`
* `Cart.customerId` → `CustomerEntity`
* `ShopOrder.customerId` → `CustomerEntity`
* `Payment.orderId` → `ShopOrderEntity`

For bidirectional cascaded relationships, child back-references are linked after mapping through parent-level `@AfterMapping` methods:

* Cart items → Cart
* Customer addresses → Customer
* Order items → ShopOrder

### 6.5 Transactional Outbox

Outbox events are persisted in the same database transaction as the aggregate change.

A scheduled process reads pending events and publishes the complete `OutboxEvent` to RabbitMQ.

This provides at-least-once delivery without distributed transactions.

`OutboxEvent` contains:

* id
* eventType
* payload
* status: `PENDING`, `SENT`, `FAILED`
* createdAt

### 6.6 Idempotency

`ShopOrder` contains an `idempotencyKey` to prevent duplicate checkout requests.

Each RabbitMQ consumer also checks the `processed_event` table before applying a message. Duplicate messages are ignored without repeating side effects.

### 6.7 Price Snapshots

`CartItem` captures product name, SKU and unit price when the item is added to the cart.

`OrderItem` captures product name, SKU and unit price when the checkout creates the order. `subtotal` is calculated and persisted.

Historical prices are therefore independent of later product price changes.

### 6.8 Stock Control

`Product` contains:

* `stockQuantity`
* `reservedQuantity`

`availableQuantity = stockQuantity - reservedQuantity`.

Stock changes are processed through the order event flow:

* `ORDER_CREATED` → reserve stock
* `ORDER_PAID` → confirm sale and decrement stock and reserved quantities
* `ORDER_CANCELLED` → release reserved stock

`InsufficientStockException` is raised when the available quantity is insufficient for a reservation.

No separate stock movement audit table is currently used; stock state is maintained through the product fields.

### 6.9 Checkout Data Integrity

Checkout requires `paymentMethod` explicitly through the full request/use-case/domain chain. `ShopOrder.fromCart()` requires the value and therefore prevents `payment_method` from being left unset.

`CartService` builds cart price snapshots server-side from the product record. Clients provide only product ID and quantity.

### 6.10 Customer Registration and Authentication

Customer registration creates the associated `User` first, using a hashed password and the `CUSTOMER` role, then persists the `Customer` with the generated user ID.

User and role persistence follows the same port/adapter pattern as other persistence operations.

Passwords are encoded before persistence and are not stored or logged in raw form.

### 6.11 Security Architecture

Spring Security uses stateless JWT authentication.

* Access token expiration: 15 minutes
* Refresh token expiration: 7 days
* Refresh tokens are rotated on refresh
* Password hashing: BCrypt

Roles:

* `CUSTOMER` — manage own cart, place and view own orders
* `ADMIN` — manage products, update order status and access reports

The JWT contains the authenticated user's email, roles and, when a customer record exists, `customerId`.

Customer-scoped routes verify that the path `customerId` matches the authenticated customer's claim. Order routes addressed by order ID or order number verify ownership against the loaded order; admins are allowed to access any order.

Customer-scoped routes (the cart routes and `GET /orders/customer/{customerId}`) do not exempt admins: an admin gets 403 there because the admin token carries no `customerId`.

An order that does not exist returns 404, while an existing order owned by someone else returns 403, so a caller can tell whether an order id exists. Order IDs are UUIDs, which makes probing impractical; returning 404 in both cases would close that if it ever matters.

Unauthenticated requests return `401`. Authenticated users without sufficient permission return `403`.

Customer `name`, `email` and `NIF` are immutable after registration.

### 6.12 Exception Handling

`GlobalExceptionHandler` centralises API exception mapping:

* `ResourceNotFoundException` → 404
* `BusinessRuleException` → 422
* `MethodArgumentNotValidException` → 400
* `DataIntegrityViolationException` → 409
* Authentication failures (`AuthenticationException`) → 401
* `JwtException` (invalid or expired token, for example on refresh) → 401
* `AccessDeniedException` → 403
* Unhandled exceptions → 500

The authentication handler must import Spring Security's `AuthenticationException` (`org.springframework.security.core`); the JNDI class of the same name (`javax.naming`) never matches a login failure.

`AccessDeniedException` needs its own handler: without it, the generic `Exception` handler turns an `AccessDeniedException` thrown inside a controller into a 500.

### 6.13 CORS

Allowed origin for the current frontend integration:

* `http://localhost:4200`

Allowed methods:

* GET
* POST
* PUT
* PATCH
* DELETE
* OPTIONS

### 6.14 Portugal Localisation

* Currency: EUR
* Tax number: NIF with Portuguese check digit validation
* Address: district and postal code (`XXXX-XXX`)
* Default country: PT
* Payment methods: `CREDIT_CARD`, `MBWAY`, `MULTIBANCO`

### 6.15 Cart Design

* Cart does not expire
* Cart is tied to an authenticated customer
* Cart items contain price snapshots
* Checkout converts the cart into a `ShopOrder`

### 6.16 Persistence and Lazy-Loading Safety

`spring.jpa.open-in-view` is disabled.

Because important relationships are LAZY, adapters that return aggregates with related data use repository queries with explicit `JOIN FETCH` where required.

The following repositories have dedicated fetch methods:

* ShopOrder items
* Product category
* Customer addresses
* Cart items

The affected adapters also reload the entity after `save()` when the mapped domain object requires the relationship to be initialized outside an open Hibernate session.

Any new caller of these adapters that runs without an open session relies on those `JOIN FETCH` queries. The Flow and Controller tests run inside `@Transactional` and would not catch a regression there; only the messaging tests exercise this path, so review new adapter callers with it in mind.

### 6.17 Update Field Preservation

`UpdateProductRequest` cannot modify SKU, status or reserved quantity.

`ProductService.updateProduct()` first loads the existing product and updates only the permitted fields, preserving immutable or system-managed values.

### 6.18 Spring Boot 4 Test Infrastructure

The project uses Spring Boot 4.0.5, which split the former monolithic test-autoconfigure module into technology-specific modules.

* Controller tests need the `spring-boot-starter-webmvc-test` dependency (test scope); it is no longer pulled in by `spring-boot-starter-test`.
* `@AutoConfigureMockMvc` is imported from `org.springframework.boot.webmvc.test.autoconfigure`.
* Jackson 3 is the default. The autoconfigured bean is `tools.jackson.databind.json.JsonMapper`; Jackson 2's `ObjectMapper` is not the autoconfigured bean, so test fields are declared as `JsonMapper`.

`MockMvc` itself remains in the Spring Test API (`org.springframework.test.web.servlet`). IntelliJ may still show a "could not autowire MockMvc" warning; it is IDE inspection lag, and `mvn test` confirms the bean is available.

### 6.19 RabbitMQ Failure Handling and DLQ

RabbitMQ uses:

* `orderflow.orders` — topic exchange
* `orderflow.notifications` — fanout exchange for notifications
* `orderflow.dlx` — fanout dead-letter exchange

Order queues are configured with a dead-letter exchange.

The listener container uses `setDefaultRequeueRejected(false)` so permanently failing messages are not requeued indefinitely and instead reach the dead-letter flow.

The dead-letter queue is explicitly bound to the fanout dead-letter exchange. The exchange is a fanout because dead-lettered messages keep their original routing key (`order.created`, `order.paid`, and so on), so a direct exchange would need one binding per key.

`RabbitAdmin` is declared as an explicit bean: Spring Boot 4 only autoconfigures `AmqpAdmin` when `spring.rabbitmq.dynamic=true`, which this project does not set.

### 6.20 Sales Reports

`GenerateSalesReportUseCase` generates an `.xlsx` file using Apache POI.

The workbook contains:

* `Orders` — one row per order in the requested date range
* `Summary` — aggregation by order status plus a total row

Cancelled orders are included in the date-range report.

The end date is inclusive for the requested calendar day. Currency cells use EUR formatting.

## 7. RabbitMQ Configuration

### Exchanges

* `orderflow.orders` — TopicExchange
* `orderflow.notifications` — FanoutExchange
* `orderflow.dlx` — FanoutExchange

### Queues

* `order.created.queue` → `order.created`
* `order.paid.queue` → `order.paid`
* `order.shipped.queue` → `order.shipped`
* `order.cancelled.queue` → `order.cancelled`
* `email.notification.queue` → fanout notification flow
* `orderflow.dead-letter.queue` → dead-letter destination

The scheduler publishes the full `OutboxEvent` object using the Jackson 3 compatible `JacksonJsonMessageConverter`.

### Messaging Consumers

* `OrderCreatedConsumer` — reserves stock
* `OrderPaidConsumer` — confirms sale
* `OrderCancelledConsumer` — releases reserved stock
* `OrderShippedConsumer` — records the processed event

## 8. Technology Stack

### Backend

* Java 26
* Spring Boot 4.0.5
* Spring Web
* Spring Data JPA
* Spring AMQP / RabbitMQ
* Spring Security
* Spring Mail
* MySQL 8
* Flyway
* MapStruct
* Apache POI
* Lombok
* SpringDoc OpenAPI

### Testing

* JUnit 5
* Mockito
* Testcontainers (MySQL + RabbitMQ)
* Awaitility for asynchronous messaging assertions

## 9. Testing Strategy

| Layer                | Type               | Tool                               | Coverage                                                     |
| -------------------- | ------------------ | ---------------------------------- | ------------------------------------------------------------ |
| Domain               | Unit               | JUnit 5                            | 53 tests (9 classes)                                         |
| Application services | Unit               | JUnit 5 + Mockito                  | 29 tests (6 classes)                                         |
| Mappers              | Unit               | JUnit 5                            | 9 tests (5 classes)                                          |
| Reports              | Unit               | JUnit 5 + Mockito                  | ReportServiceTest, 5 tests                                   |
| Repositories         | Integration        | Testcontainers                     | 29 tests (5 flow suites)                                     |
| Controllers          | Integration        | Spring Boot Test + MockMvc         | 56 tests (7 classes, real JWTs, includes ReportControllerTest) |
| Messaging            | Integration        | Testcontainers + RabbitMQ          | MessagingFlowIntegrationTest, 7 tests                        |

Integration tests use real MySQL and RabbitMQ containers. Messaging tests run without `@Transactional` because consumers use a separate database session.

The test suite uses a singleton-container pattern so the same MySQL and RabbitMQ containers remain available across test classes.

Full suite: 189 tests passing (96 unit, 1 application-context load, 29 persistence integration, 56 controller integration, 7 messaging integration).

## 10. Database Indexes

| Table        | Index           | Purpose                  |
| ------------ | --------------- | ------------------------ |
| product      | category_id     | Category filtering       |
| product      | status          | Active product queries   |
| shop_order   | customer_id     | Customer order history   |
| shop_order   | status          | Order management queries |
| shop_order   | idempotency_key | Duplicate prevention     |
| cart         | customer_id     | Customer cart lookup     |
| outbox_event | status          | Pending event polling    |