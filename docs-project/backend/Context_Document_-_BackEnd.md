# order-flow — Architecture Context Document

## 1. Final Architecture Plan

This project follows Hexagonal Architecture (Ports and Adapters) to ensure
a clear separation between:

- Domain logic
- Application use cases
- Infrastructure
- External integrations

The core business logic resides entirely within the domain layer and is
independent of frameworks. External systems such as databases, message brokers,
and web controllers communicate with the domain exclusively through ports.

## 2. Domain Model

### Aggregates

- Product
- Customer
- Cart
- ShopOrder
- Payment

### Value Objects

- Money (amount + currency, default EUR)
- Email (validated, lowercase enforced)
- NIF (9 digits, check digit validated — Portuguese tax number)

### Supporting Entities

- Category (belongs to Product domain)
- Address (belongs to Customer domain)
- CartItem
- OrderItem
- OutboxEvent

### Domain Events

- OrderCreatedEvent
- OrderPaidEvent
- OrderCancelledEvent
- OrderShippedEvent
- OrderStatusChangedEvent

### Domain Exceptions

- DomainException (abstract base)
- ResourceNotFoundException → HTTP 404
- BusinessRuleException → HTTP 422
- ProductNotFoundException
- CustomerNotFoundException
- CartNotFoundException
- OrderNotFoundException
- InsufficientStockException
- InvalidOrderStatusTransitionException

## 3. Order State Machine

Allowed transitions:

- PENDING → PAID, CANCELLED
- PAID → PREPARING
- PREPARING → SHIPPED, CANCELLED
- SHIPPED → DELIVERED

Transitions are validated inside the ShopOrder aggregate.
Invalid transitions throw InvalidOrderStatusTransitionException.

## 4. Package Structure

orderflow/
└── src/main/java/com/camilagksantos/orderflow/
├── domain/
│ ├── auth/ ← User, Role
│ ├── cart/ ← Cart, CartItem, CartStatus
│ ├── customer/ ← Customer, Address, CustomerStatus
│ ├── event/ ← DomainEvent, domain events, OutboxEvent
│ ├── exception/ ← domain exceptions
│ ├── order/ ← ShopOrder, OrderItem, OrderStatus, PaymentMethod
│ ├── payment/ ← Payment, PaymentStatus
│ ├── product/ ← Product, Category, ProductStatus
│ └── shared/ ← Money, Email, NIF
├── application/
│ ├── port/
│ │ ├── input/ ← use case interfaces
│ │ └── output/ ← repository and event interfaces
│ ├── service/ ← use case implementations
│ ├── dto/ ← request / response models
│ └── mapper/ ← domain ↔ dto mappers
└── infrastructure/
├── adapter/
│ ├── input/
│ │ ├── web/ ← REST controllers
│ │ └── messaging/ ← RabbitMQ consumers
│ └── output/
│ ├── persistence/ ← JPA adapters
│ ├── messaging/ ← RabbitMQ publisher
│ └── email/ ← MailHog adapter
├── persistence/
│ ├── entity/ ← JPA entities
│ ├── repository/ ← Spring Data JPA interfaces
│ └── mapper/ ← domain ↔ entity mappers
└── config/ ← Spring beans, RabbitMQ, OpenAPI, Security

## 5. Environment Profiles

### dev

- MySQL 8 via Docker
- RabbitMQ via Docker
- MailHog via Docker
- Used for local development

### test

- Testcontainers (MySQL + RabbitMQ)
- Spins up real containers per test suite
- Used for integration tests

### prod

- MySQL 8 (external)
- RabbitMQ (external)
- SMTP (external)
- Configuration via environment variables

## 6. Key Architectural Decisions

### 6.1 Pure Hexagonal Architecture

The domain layer contains no dependencies on frameworks.
Spring Boot, JPA, RabbitMQ and all external libraries are restricted
to infrastructure and adapter layers.
Domain models are plain Java records with no annotations.

### 6.2 Domain Models as Lombok Classes

Domain models are implemented as regular Java classes with Lombok annotations
(@Getter, @Setter, @Builder, @NoArgsConstructor, @AllArgsConstructor).

Reason for this choice:
Java records exposed behaviour methods (reserve, ship, cancel, addItem) as
mappable properties to MapStruct, generating unmapped property warnings and
requiring excessive @Mapping(ignore = true) annotations for every method.
Migrating to Lombok classes eliminated this complexity entirely.

Domain models remain framework-free — no JPA, Spring or external annotations.
Behaviour methods mutate the object state directly (void return) instead of
returning new instances, following a more pragmatic and maintainable approach.

DTOs are implemented as Java records — immutable transfer objects with no
behaviour are the ideal use case for records.

### 6.3 JPA Entity Mapping

- Located in infrastructure/persistence/entity/
- Implemented as regular classes with Lombok
- Lazy loading for all relationships
- Enums mapped using EnumType.STRING
- Auto-generated IDs using GenerationType.IDENTITY for Long PKs
- String UUIDs for Cart, CartItem, ShopOrder, OrderItem, Payment PKs

### 6.4 Mapper Strategy

Two types of mappers:

Application Mappers (application/mapper/)

- DTO ↔ Domain
- Used by controllers and services

Persistence Mappers (infrastructure/persistence/mapper/)

- Entity ↔ Domain
- Used by persistence adapters

MapStruct is used for all mappings — compile-time code generation, type-safe, no runtime reflection.

Type conversion is handled via default methods declared directly in each mapper interface.
MapStruct detects the conversion automatically by matching method signatures — no @Named or qualifiedByName needed.

Conversion methods used:

- BigDecimal → Money: default Money toMoney(BigDecimal value)
- Money → BigDecimal: default BigDecimal toBigDecimal(Money money)
- String → Email: default Email toEmail(String value)
- Email → String: default String fromEmail(Email email)
- String → NIF: default NIF toNIF(String value)
- NIF → String: default String fromNIF(NIF nif)

Ignored fields in toEntity() mappings:

- createdAt, updatedAt — managed by @PrePersist / @PreUpdate
- Direct-owning relationship fields with no cascade dependency are set via
  shallow entity references built from the raw domain ID, not truly "ignored"
  — see 6.15 for the full pattern. Only child-side back-references in
  bidirectional cascaded relations (e.g. CartItemEntity.cart) remain genuinely
  ignored, resolved instead via @AfterMapping on the parent mapper.

Response DTOs use Money directly instead of BigDecimal — richer representation
in JSON responses (amount + currency) and easier to identify in logs.

Request DTOs use BigDecimal for price fields — clients send simple numeric values,
not Money objects.

boolean fields renamed from isX to xBoolean pattern (e.g. isDefault → defaultAddress)
to avoid MapStruct/Lombok getter conflict where Lombok generates isX() getter
and MapStruct interprets the property name as x instead of isX.

### 6.5 Transactional Outbox Pattern

Events are persisted in the same database transaction as the aggregate.
A scheduler reads pending OutboxEvents and publishes them to RabbitMQ.
Guarantees at-least-once delivery without distributed transactions.

OutboxEvent fields:

- id: UUID
- eventType: ORDER_CREATED, ORDER_PAID, ORDER_CANCELLED, ORDER_SHIPPED
- payload: JSON serialized event data
- status: PENDING, SENT, FAILED
- createdAt: timestamp

### 6.6 Idempotency

ShopOrder carries an idempotencyKey (UUID).
Each RabbitMQ consumer checks the processed_event table before handling a message.
Duplicate messages are safely ignored without side effects.

### 6.7 Price Snapshots

CartItem and OrderItem store snapshots of product name, SKU and price
at the moment of the action.
Price changes never affect historical cart or order records.

### 6.8 Stock Control

Product has two fields: stockQuantity and reservedQuantity.
availableQuantity = stockQuantity - reservedQuantity.
Stock is controlled synchronously on order creation and cancellation.
InsufficientStockException is thrown when availableQuantity < requested quantity.

### 6.9 Security Architecture

Spring Security with JWT-based authentication.
UserDetails is implemented in the infrastructure layer (UserDetailsImpl)
to avoid coupling the domain User record to the framework.

Roles:

- CUSTOMER: manage own cart, place and view own orders
- ADMIN: manage products, update order status, access reports

Customer data immutability:

- name, email and NIF cannot be changed after registration
- Enforced at the domain level

### 6.10 Exception Handling

Centralised via @ControllerAdvice (GlobalExceptionHandler).

Exception mapping:

- 404: ResourceNotFoundException
- 422: BusinessRuleException
- 400: MethodArgumentNotValidException
- 409: DataIntegrityViolationException
- 500: Generic Exception

### 6.11 CORS Configuration

- Allowed origin: http://localhost:4200
- Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS

### 6.12 Portugal Localisation

- Currency: EUR (default in Money value object)
- Tax number: NIF (9 digits with check digit validation)
- Address format: district and postal code (XXXX-XXX)
- Country default: PT
- Payment methods: CREDIT_CARD, MBWAY, MULTIBANCO

### 6.13 Cart Design

- Cart never expires (conscious product decision)
- Cart is always tied to an authenticated customer
- CartItem stores price snapshot at time of addition
- Cart converts to ShopOrder on checkout

### 6.14 Customer-User Mapping (MapStruct)

Customer.userId (Long, domain) maps to CustomerEntity.user (UserEntity, @OneToOne LAZY).

Since these types are incompatible for direct MapStruct auto-mapping, explicit
default conversion methods are declared in CustomerPersistenceMapper:

- Long -> UserEntity: creates a "shallow" UserEntity with only the id set
  (equivalent to EntityManager.getReference() semantics), sufficient for
  Hibernate to resolve the FK on insert/update without loading the full User.
- UserEntity -> Long: extracts the id directly.

This assumes the referenced User already exists in the database at the time
Customer is persisted — true for the current registration flow (User is
always created before Customer). If Customer creation without a pre-existing
User is ever required, the FK constraint will correctly reject the insert.

Known pitfall: @Mapping(target = "user", ignore = true) silently drops the
user_id value with no compilation error and no runtime exception — MapStruct
simply leaves the field null. Since user_id is NOT NULL UNIQUE at the schema
level, this only surfaces as a DataIntegrityViolationException at persistence
time, not at the domain or mapping layer. Any @Mapping(ignore = true) on a
NOT NULL column should be treated as a red flag during review.

### 6.15 MapStruct FK Reference Pattern

Applied consistently across persistence mappers whenever a domain object
holds a raw ID (Long/String) but the corresponding JPA entity expects a
full related entity object for a @ManyToOne/@OneToOne relationship:

Simple relations (no cascade): a default conversion method builds a
"shallow" entity instance with only the id set (equivalent to
EntityManager.getReference() semantics) — sufficient for Hibernate to
resolve the FK on insert/update.

- Customer.userId -> UserEntity (CustomerPersistenceMapper)
- Cart.customerId -> CustomerEntity (CartPersistenceMapper)
- ShopOrder.customerId -> CustomerEntity (ShopOrderPersistenceMapper)
- Payment.orderId -> ShopOrderEntity (PaymentPersistenceMapper)

Bidirectional relations with cascade (@OneToMany mappedBy + CascadeType.ALL):
the child mapper still ignores the back-reference (e.g. cart in
CartItemPersistenceMapper), but the parent mapper adds an @AfterMapping
method that iterates the mapped children and sets the back-reference
after the object graph is built, before persistence.

- CartPersistenceMapper.linkItemsToCart() -> CartItemEntity.cart
- CustomerPersistenceMapper.linkAddressesToCustomer() -> AddressEntity.customer
- ShopOrderPersistenceMapper.linkItemsToOrder() -> OrderItemEntity.order

Known pitfall this replaces: @Mapping(target = "x", ignore = true) on a
NOT NULL FK column silently persists null with no compile-time or
mapping-time error — it only surfaces as a DataIntegrityViolationException
at the database layer. Any ignored relationship field should be checked
against the schema's NOT NULL constraints during review.

### 6.16 Checkout PaymentMethod Propagation

Bug found: OrderService.checkout() called ShopOrder.fromCart() without ever
setting paymentMethod, despite shop_order.payment_method being NOT NULL.
CheckoutRequest and CheckoutUseCase never carried paymentMethod at all, so
the data was missing from the point of entry, not just forgotten in one method.

Fix applied across the full chain: CheckoutRequest gained a required
paymentMethod field; CheckoutUseCase.checkout() signature extended with a
PaymentMethod parameter; CartController passes it through; ShopOrder.fromCart()
itself now requires paymentMethod as a parameter, consistent with how
idempotencyKey and customerEmail — data that cannot be derived from the Cart —
are already required parameters of that factory.

### 6.17 Customer Registration and User/Role Creation

Bug found: CustomerService.registerCustomer() only saved the Customer,
never creating a User account. RegisterCustomerRequest had no password field,
CustomerMapper ignored userId entirely, and no endpoint anywhere in the
system created a User — meaning every call to POST /api/v1/customers would
fail on the customer.user_id NOT NULL UNIQUE constraint, and even if it
hadn't, the registered customer would have had no way to ever log in.

This also surfaced a gap in the hexagonal architecture itself: User and Role
persistence bypassed the port/adapter pattern entirely, accessed directly via
UserJpaRepository from UserDetailsServiceImpl and JwtAuthenticationFilter.

Fix:

- RegisterCustomerRequest gained a required password field
- New output ports: UserRepositoryPort, RoleRepositoryPort
- New adapters: UserJpaAdapter, RoleJpaAdapter (following the same pattern as
  all other aggregates)
- RoleJpaRepository gained findByName(String)
- RegisterCustomerUseCase.registerCustomer() signature extended to accept
  the raw password
- CustomerService now: looks up the CUSTOMER role, hashes the password via
  PasswordEncoder, builds and saves a User with that role, then sets
  customer.userId from the saved User's id before saving the Customer
- New migration V4\_\_seed_roles.sql seeds ADMIN and CUSTOMER roles
  (INSERT IGNORE, safe against pre-existing data)

Password is never persisted or logged in raw form — it exists only in memory
between the HTTP request and the PasswordEncoder.encode() call.

### 6.18 Testcontainers Singleton Container Pattern

Bug found: BaseIntegrationTest originally declared MySQL and RabbitMQ
containers as static fields annotated with @Container. Since @Container
ties container lifecycle to JUnit's callback management, and the field is
static (shared across all subclasses via inheritance), the first integration
test class to finish caused JUnit to stop the containers — leaving every
subsequent test class in the same Maven run connecting to dead containers
(Connection refused / HikariPool timeout errors).

Fix: containers are now declared as plain static final fields (no @Container
annotation) and started manually in a static initializer block:

    static { mysql.start(); rabbitMQ.start(); }

This is the Testcontainers-recommended "singleton container" pattern for
suites with multiple test classes sharing infrastructure. A single MySQL and
RabbitMQ instance now serves the entire test run, torn down only by Ryuk at
JVM exit — not by JUnit's per-class afterAll lifecycle. @Transactional on each
test class remains essential, since the schema is no longer reset between
classes — rollback isolation is the only thing preventing cross-test data
leakage.

### 6.19 Cart Price Snapshot Enforcement

Bug found: CartController.addItem() built a CartItem from client input containing
only productId and quantity, never populating unitPrice, productName, or productSku.
This violated the price snapshot decision (6.7) and caused a NullPointerException
whenever an item was persisted, since CartItem.unitPrice is required for the
CartItemPersistenceMapper's Money -> BigDecimal conversion.

The client legitimately should never supply price, name, or SKU — allowing that
would let a client dictate its own price, a serious business logic flaw.

Fix: AddToCartUseCase.addToCart() now takes (customerId, productId, quantity)
instead of a pre-built CartItem. CartService fetches the Product via
ProductRepositoryPort and builds the CartItem snapshot server-side before
adding it to the cart. CartController no longer constructs CartItem directly.

### 6.20 Product Update Field Preservation

Bug found: ProductService.updateProduct() saved the incoming Product object
directly, without first loading the existing record. Since UpdateProductRequest
has no sku field (sku is immutable by design, per 6.2 DTO decisions), the
mapper-built Product always had sku = null, causing every update to fail
the sku NOT NULL constraint.

Fix: updateProduct() now loads the existing Product first, then applies only
the fields UpdateProductRequest is allowed to change (name, description, price,
stockQuantity, category, imageUrl), preserving id, sku, status, and
reservedQuantity from the existing record.

### 6.21 Authentication Entry Point (401 vs 403)

Bug found: SecurityConfig defined no explicit AuthenticationEntryPoint, so
Spring Security fell back to its default Http403ForbiddenEntryPoint — meaning
requests with no token at all received 403 Forbidden instead of 401 Unauthorized,
conflating "not authenticated" with "authenticated but not authorized."

Fix: SecurityConfig now configures exceptionHandling with a custom
AuthenticationEntryPoint that returns 401 for any unauthenticated request.
GlobalExceptionHandler also gained a handler for AuthenticationException
(covering login's BadCredentialsException), returning 401 with a generic
"Invalid credentials" message instead of falling through to the 500 handler.

Known gap: JWT parsing failures during token refresh (JwtException from
jsonwebtoken, not a Spring Security AuthenticationException) still fall
through to the generic 500 handler. Not yet fixed — flagged for future work.

### 6.22 Response DTO Field Completeness

Bug found: OrderResponse was missing a cancelReason field despite
ShopOrder.cancelReason existing in the domain. Since MapStruct maps by name
automatically, a field simply absent from the DTO is silently dropped from
every response with no error — the cancellation reason was persisted correctly
but never returned to API clients.

Lesson: when adding a domain field, response DTO parity should be checked
explicitly, since nothing fails at compile or mapping time when a DTO field
is missing — only a specific consumer noticing the gap.

### 6.23 Spring Boot 4 Module System Migration Notes

Spring Boot 4.0 split the former monolithic spring-boot-autoconfigure and
spring-boot-test-autoconfigure jars into dozens of small, technology-specific
modules. This project (on 4.0.5) hit three concrete breaking changes when
writing controller integration tests:

1. @AutoConfigureMockMvc moved from
   org.springframework.boot.test.autoconfigure.web.servlet to
   org.springframework.boot.webmvc.test.autoconfigure, and requires an
   explicit test dependency: spring-boot-starter-webmvc-test (added to pom.xml,
   test scope). It is no longer pulled in transitively by spring-boot-starter-test.

2. Jackson 3 is now the default JSON library. The autoconfigured bean is
   tools.jackson.databind.json.JsonMapper, not com.fasterxml.jackson.databind.ObjectMapper.
   Both classes coexist on the classpath (Jackson 2 kept for compatibility), which
   IDEs may present ambiguously. All controller test fields now declare
   `JsonMapper objectMapper` instead of `ObjectMapper objectMapper` — the API
   (writeValueAsString, readTree) is unchanged, only the type.

3. org.springframework.test.web.servlet.MockMvc itself did NOT move — false
   "cannot autowire" warnings seen in IntelliJ for this class were IDE inspection
   lag, not real errors; verified via successful `mvn test` runs.

Reference: official Spring Boot 4.0 Migration Guide
(github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide),
"Module Dependencies" / "Test Code" sections.

Verified fix: full suite (167 tests: 91 unit + 29 persistence integration +
47 controller integration) passes cleanly in a single `mvn test` run after
this change.

## 7. RabbitMQ Configuration

### Exchanges

- orderflow.orders (topic)
- orderflow.notifications (fanout)
- orderflow.dlx (dead letter)

### Queues

- order.created.queue → routing key: order.created
- order.paid.queue → routing key: order.paid
- order.shipped.queue → routing key: order.shipped
- order.cancelled.queue → routing key: order.cancelled
- email.notification.queue
- orderflow.dead-letter.queue

### Dead Letter Queue

Failed messages after max retry attempts are routed to orderflow.dlx.
Allows inspection and replay without data loss.

## 8. Technology Stack

### Backend

- Java 26
- Spring Boot 4.0.5
- Spring Web
- Spring Data JPA
- Spring AMQP (RabbitMQ)
- Spring Security
- Spring Mail
- MySQL 8
- Flyway
- MapStruct
- Apache POI
- Lombok
- SpringDoc OpenAPI

### Testing

- JUnit 5
- Mockito
- Testcontainers (MySQL + RabbitMQ)

## 9. Testing Strategy

| Layer                | Type        | Tool              | Target |
| -------------------- | ----------- | ----------------- | ------ |
| Domain models        | Unit        | JUnit 5           | 90%+   |
| Application services | Unit        | JUnit 5 + Mockito | 85%+   |
| Controllers          | Integration | @SpringBootTest   | 80%+   |
| RabbitMQ consumers   | Integration | Testcontainers    | 80%+   |
| Repositories         | Integration | Testcontainers    | 80%+   |

## 10. Database Indexes

| Table        | Index           | Purpose                  |
| ------------ | --------------- | ------------------------ |
| product      | category_id     | category filtering       |
| product      | status          | active product queries   |
| shop_order   | customer_id     | customer order history   |
| shop_order   | status          | order management queries |
| shop_order   | idempotency_key | duplicate prevention     |
| cart         | customer_id     | customer cart lookup     |
| outbox_event | status          | pending event polling    |
