# order-flow — Implementation Progress Document (Backend)

## 1. Implemented Areas

- Database migrations
- Application configuration
- Domain models
- Domain exceptions
- Value objects
- JPA entities
- Input and output ports
- Application services
- JPA repositories
- Persistence mappers
- Persistence adapters
- DTOs (request / response)
- Application mappers
- REST controllers
- RabbitMQ configuration
- Messaging publisher and scheduler
- Messaging consumers
- Email adapter
- Exception handling
- OpenAPI configuration

## 2. Database Migrations

Files:

- V1\_\_create_schema.sql
- V2\_\_rename_address_is_default_column.sql
- V3\_\_add_customer_email_to_shop_order.sql

Key Decisions:

- All tables in singular form following modern JPA convention
- shop_order used instead of order (reserved word in MySQL)
- String UUIDs as primary keys for cart, cart_item, shop_order, order_item, payment, outbox_event, processed_event
- Auto-increment BIGINT for role, user, category, product, customer, address
- Portugal localisation: NIF instead of CPF, district instead of state, postal_code instead of zip_code, country default PT
- Cart has no expiration — conscious product decision

## 3. Application Configuration

Files:

- application.yaml

Key Decisions:

- spring.jpa.open-in-view: false
- spring.jpa.hibernate.ddl-auto: validate
- Hibernate dialect removed — auto-detected by Hibernate 7
- MySQL, RabbitMQ and MailHog configured for local Docker environment
- app.mail.from externalised to application.yaml — no hardcoded values in adapters

## 4. Domain Models

Domain models are implemented as Lombok classes (@Getter, @Setter, @Builder,
@NoArgsConstructor, @AllArgsConstructor) — framework-free, no JPA annotations.

Reason for migration from records:
Records exposed behaviour methods as mappable properties to MapStruct, requiring
excessive ignore annotations. Lombok classes eliminated this complexity entirely.

Value Objects (domain/shared/):

- Money — amount (BigDecimal) + currency (String, default EUR), with add/subtract/multiply operations
- Email — validated by regex, lowercase enforced on construction (record)
- NIF — 9 digits, check digit validated using Portuguese algorithm (record)

Note: Money, Email and NIF remain as records — they are pure value objects
with no behaviour methods that would conflict with MapStruct.

Aggregates:

- Product — reserve/release/activate/deactivate/confirmSale (void mutations)
- Customer — block/activate (void mutations)
- Cart — addItem/removeItem/convert (void mutations) + newCart (static factory)
- ShopOrder — pay/startPreparing/ship/deliver/cancel (void mutations) + fromCart (static factory)
- Payment — approve/decline (void mutations)

Supporting Entities:

- Category (domain/category/)
- Address (domain/customer/)
- CartItem — stores price snapshot at time of addition
- OrderItem — stores price snapshot at time of checkout, includes subtotal as persisted field
- OutboxEvent

Enums:

- ProductStatus: ACTIVE, INACTIVE, DISCONTINUED
- CustomerStatus: ACTIVE, INACTIVE, BLOCKED
- CartStatus: ACTIVE, CONVERTED, ABANDONED
- OrderStatus: PENDING, PAID, PREPARING, SHIPPED, DELIVERED, CANCELLED
- PaymentMethod: CREDIT_CARD, MBWAY, MULTIBANCO
- PaymentStatus: PENDING, PROCESSING, APPROVED, DECLINED
- OutboxEventStatus: PENDING, SENT, FAILED

Domain Events (domain/event/):

- DomainEvent (interface)
- OrderCreatedEvent
- OrderPaidEvent
- OrderCancelledEvent
- OrderShippedEvent
- OrderStatusChangedEvent

Auth Models (domain/auth/):

- User — activate/deactivate (void mutations), hasRole check
- Role

## 5. Domain Exceptions

Hierarchy:

- DomainException (abstract base — extends RuntimeException)
  - ResourceNotFoundException → HTTP 404
    - ProductNotFoundException
    - CustomerNotFoundException
    - CartNotFoundException
    - OrderNotFoundException
  - BusinessRuleException → HTTP 422
    - InsufficientStockException
    - InvalidOrderStatusTransitionException

## 6. JPA Entities

Located in infrastructure/persistence/entity/.
Implemented as regular classes with Lombok (@Getter, @Setter, @NoArgsConstructor).
All relationships use FetchType.LAZY except UserEntity.roles (EAGER — required by Spring Security).
Enums mapped using EnumType.STRING.
@PrePersist and @PreUpdate used for automatic timestamp management.

Entities:

- RoleEntity → table: role
- UserEntity → table: user (roles: ManyToMany EAGER)
- CategoryEntity → table: category
- ProductEntity → table: product (category: ManyToOne LAZY)
- CustomerEntity → table: customer (user: OneToOne LAZY, addresses: OneToMany LAZY CascadeAll)
- AddressEntity → table: address (customer: ManyToOne LAZY)
- CartEntity → table: cart (customer: ManyToOne LAZY, items: OneToMany LAZY CascadeAll orphanRemoval)
- CartItemEntity → table: cart_item (cart: ManyToOne LAZY)
- ShopOrderEntity → table: shop_order (customer: ManyToOne LAZY, items: OneToMany LAZY CascadeAll)
- OrderItemEntity → table: order_item (order: ManyToOne LAZY)
- PaymentEntity → table: payment (order: OneToOne LAZY)
- OutboxEventEntity → table: outbox_event
- ProcessedEventEntity → table: processed_event

Key Decisions:

- CartEntity uses orphanRemoval = true — cart items do not exist outside a cart
- ShopOrderEntity does NOT use orphanRemoval — order items are historical records

## 7. Output Ports

Located in application/port/output/.
Define what the application needs from the outside world.

Ports:

- ProductRepositoryPort — save, findById, findBySku, findAll, findByCategoryId, deleteById
- CategoryRepositoryPort — save, findById, findAll
- CustomerRepositoryPort — save, findById, findByEmail, findByNif
- CartRepositoryPort — save, findById, findActiveByCustomerId
- OrderRepositoryPort — save, findById, findByOrderNumber, findByIdempotencyKey, findByCustomerId
- PaymentRepositoryPort — save, findByOrderId
- OutboxEventRepositoryPort — save, findByStatus, updateStatus
- ProcessedEventRepositoryPort — existsById, save
- EventPublisherPort — publish(DomainEvent)
- EmailNotificationPort — sendOrderConfirmation, sendOrderShipped, sendOrderCancelled

## 8. Input Ports (Use Cases)

Located in application/port/input/.
Define what the application can do from the outside world's perspective.
One interface per use case following Interface Segregation Principle.
Method names are descriptive — no generic execute() pattern.

Use Cases:

- CreateProductUseCase — createProduct
- FindProductUseCase — findProductById, findProductBySku, findAllProducts, findProductsByCategoryId
- UpdateProductUseCase — updateProduct
- DeleteProductUseCase — deleteProduct
- CreateCategoryUseCase — createCategory
- FindCategoryUseCase — findCategoryById, findAllCategories
- RegisterCustomerUseCase — registerCustomer
- FindCustomerUseCase — findCustomerById
- AddToCartUseCase — addToCart
- RemoveFromCartUseCase — removeFromCart
- FindCartUseCase — findCartByCustomerId
- CheckoutUseCase — checkout
- FindOrderUseCase — findOrderById, findOrderByNumber, findOrdersByCustomerId
- UpdateOrderStatusUseCase — updateOrderStatus
- CancelOrderUseCase — cancelOrder
- ProcessPaymentUseCase — processPayment
- GenerateSalesReportUseCase — generateSalesReport

## 9. Application Services

Located in application/service/.
Implement input ports and depend exclusively on output ports.
Spring @Service + Lombok @RequiredArgsConstructor for dependency injection.

Services:

- CategoryService — implements CreateCategoryUseCase, FindCategoryUseCase
- ProductService — implements CreateProductUseCase, FindProductUseCase, UpdateProductUseCase, DeleteProductUseCase
- CustomerService — implements RegisterCustomerUseCase, FindCustomerUseCase
- CartService — implements AddToCartUseCase, RemoveFromCartUseCase, FindCartUseCase
- OrderService — implements CheckoutUseCase, FindOrderUseCase, UpdateOrderStatusUseCase, CancelOrderUseCase
- PaymentService — implements ProcessPaymentUseCase
- ReportService — implements GenerateSalesReportUseCase (placeholder — returns empty byte[])

Key Decisions:

- OrderService.checkout() is @Transactional — order creation and outbox event persist atomically
- CartService.addToCart() creates a new cart if none exists for the customer
- ShopOrder.fromCart() converts cart items to order items as price snapshots at checkout time
- Domain behaviour methods are void — state is mutated directly, then saved via repository
- CheckoutUseCase.checkout() requires paymentMethod as an explicit parameter — ShopOrder.fromCart() enforces it as a required argument, preventing the payment_method NOT NULL column from ever being left unset

## 10. JPA Repositories

Located in infrastructure/persistence/repository/.
Extend JpaRepository — Spring Data generates implementation at runtime.

Repositories:

- RoleJpaRepository — JpaRepository<RoleEntity, Long>
- UserJpaRepository — findByEmail
- CategoryJpaRepository — JpaRepository<CategoryEntity, Long>
- ProductJpaRepository — findBySku, findByCategoryId
- CustomerJpaRepository — findByEmail, findByNif
- AddressJpaRepository — findByCustomerId
- CartJpaRepository — findByCustomerIdAndStatus
- ShopOrderJpaRepository — findByOrderNumber, findByIdempotencyKey, findByCustomerId
- PaymentJpaRepository — findByOrderId
- OutboxEventJpaRepository — findByStatus
- ProcessedEventJpaRepository — JpaRepository<ProcessedEventEntity, String>

## 11. Persistence Mappers

Located in infrastructure/persistence/mapper/.
MapStruct interfaces — implementation generated at compile time.
Type conversions via default methods in each mapper — MapStruct detects automatically by signature.

Mappers:

- RolePersistenceMapper — Role ↔ RoleEntity (direct field mapping)
- UserPersistenceMapper — User ↔ UserEntity (uses RolePersistenceMapper)
- CategoryPersistenceMapper — Category ↔ CategoryEntity (direct field mapping)
- ProductPersistenceMapper — Product ↔ ProductEntity (toMoney/toBigDecimal default methods)
- AddressPersistenceMapper — Address ↔ AddressEntity (customer.id → customerId)
- CustomerPersistenceMapper — Customer ↔ CustomerEntity (toEmail/fromEmail/toNIF/fromNIF default methods)
- CartItemPersistenceMapper — CartItem ↔ CartItemEntity (toMoney/toBigDecimal default methods, cart.id → cartId)
- CartPersistenceMapper — Cart ↔ CartEntity (customer.id → customerId, uses CartItemPersistenceMapper)
- OrderItemPersistenceMapper — OrderItem ↔ OrderItemEntity (toMoney/toBigDecimal default methods, order.id → orderId)
- ShopOrderPersistenceMapper — ShopOrder ↔ ShopOrderEntity (uses OrderItemPersistenceMapper)
- PaymentPersistenceMapper — Payment ↔ PaymentEntity (toMoney/toBigDecimal default methods, order.id → orderId)
- OutboxEventPersistenceMapper — OutboxEvent ↔ OutboxEventEntity (direct mapping)

Key Decisions:

- createdAt and updatedAt ignored in toEntity() — managed by @PrePersist / @PreUpdate
- Relationship fields (customer, cart, order) ignored in toEntity() — set by JPA cascade
- Type conversions via default methods — no expressions, no @Named, MapStruct detects by signature
- isDefault renamed to defaultAddress — avoids Lombok/MapStruct conflict with boolean isX() getter pattern
- OrderItem.subtotal added as real field — calculated on fromCart() and persisted, avoids expression in mapper
- ShopOrderPersistenceMapper does not declare toMoney/toBigDecimal — inherits from OrderItemPersistenceMapper via uses

## 12. Persistence Adapters

Located in infrastructure/adapter/output/persistence/.
Implement output ports using JPA repositories and persistence mappers.
Annotated with @Component — Spring registers them as beans.

Adapters:

- CategoryJpaAdapter — implements CategoryRepositoryPort
- ProductJpaAdapter — implements ProductRepositoryPort
- CustomerJpaAdapter — implements CustomerRepositoryPort
- CartJpaAdapter — implements CartRepositoryPort (uses CartStatus.ACTIVE for findActiveByCustomerId)
- OrderJpaAdapter — implements OrderRepositoryPort
- PaymentJpaAdapter — implements PaymentRepositoryPort
- OutboxEventJpaAdapter — implements OutboxEventRepositoryPort
- ProcessedEventJpaAdapter — implements ProcessedEventRepositoryPort

## 13. DTOs

Located in application/dto/.
Implemented as Java records — immutable, no behaviour, ideal for transfer objects.

Request DTOs (application/dto/request/):

- CreateCategoryRequest — name
- CreateProductRequest — name, description, sku, price, stockQuantity, categoryId, imageUrl
- UpdateProductRequest — name, description, price, stockQuantity, categoryId, imageUrl (no sku — immutable after creation)
- RegisterCustomerRequest — name, email, nif, phone
- CreateAddressRequest — street, number, complement, neighborhood, city, district, postalCode
- AddToCartRequest — productId, quantity
- CheckoutRequest — idempotencyKey, addressId, paymentMethod
- UpdateOrderStatusRequest — status
- CancelOrderRequest — reason
- ProcessPaymentRequest — orderId, method, cardLastFour, cardBrand, mbwayPhone, mbEntity, mbReference
- LoginRequest — email, password

Response DTOs (application/dto/response/):

- CategoryResponse — id, name
- ProductResponse — id, name, description, sku, price (Money), stockQuantity, reservedQuantity, availableQuantity, category, imageUrl, status
- AddressResponse — id, street, number, complement, neighborhood, city, district, postalCode, country, defaultAddress
- CustomerResponse — id, name, email, nif, phone, status, addresses
- CartItemResponse — id, productId, productName, productSku, unitPrice (Money), quantity, subtotal (Money)
- CartResponse — id, customerId, status, items, total (Money)
- OrderItemResponse — id, productId, productName, productSku, unitPrice (Money), quantity, subtotal (Money)
- OrderResponse — id, orderNumber, customerId, status, items, subtotal (Money), shippingCost (Money), discountAmount (Money), totalAmount (Money), paymentMethod, trackingCode, timestamps
- PaymentResponse — id, orderId, amount (Money), method, status, transactionId, processedAt, createdAt
- TokenResponse — accessToken, refreshToken, tokenType, expiresIn
- ErrorResponse — status, error, message, path, timestamp

Key Decisions:

- One DTO class per operation where fields differ (CreateProduct vs UpdateProduct — sku immutable)
- Single DTO class when fields are identical across operations
- Response DTOs use Money directly — richer JSON representation and easier log identification
- Request DTOs use BigDecimal for price — clients send simple numeric values

## 14. Application Mappers

Located in application/mapper/.
MapStruct interfaces — DTO ↔ Domain conversion.
Same default method pattern as persistence mappers.

Mappers:

- CategoryMapper — CategoryResponse ← Category, Category ← CreateCategoryRequest
- ProductMapper — ProductResponse ← Product, Product ← CreateProductRequest/UpdateProductRequest (toMoney default method for BigDecimal → Money)
- AddressMapper — AddressResponse ← Address, Address ← CreateAddressRequest
- CustomerMapper — CustomerResponse ← Customer, Customer ← RegisterCustomerRequest (toEmail/fromEmail/toNIF/fromNIF default methods)
- CartMapper — CartResponse ← Cart, CartItemResponse ← CartItem (total and subtotal via expression — calculated methods)
- OrderMapper — OrderResponse ← ShopOrder, OrderItemResponse ← OrderItem
- PaymentMapper — PaymentResponse ← Payment

Key Decisions:

- availableQuantity mapped via expression in ProductMapper — calculated method, not a stored field
- CartMapper uses expression for total and subtotal — Cart.total() and CartItem.subtotal() are calculated methods

## 15. REST Controllers

Located in infrastructure/adapter/input/web/.
Receive HTTP requests and delegate to use cases.
Annotated with @RestController and @RequiredArgsConstructor.

Controllers:

- CategoryController — POST /api/v1/categories, GET /api/v1/categories, GET /api/v1/categories/{id}
- ProductController — POST, GET, GET/{id}, GET/sku/{sku}, GET/category/{categoryId}, PUT/{id}, DELETE/{id}
- CustomerController — POST /api/v1/customers, GET /api/v1/customers/{id}
- CartController — GET /customer/{customerId}, POST /customer/{customerId}/items, DELETE /customer/{customerId}/items/{itemId}, POST /customer/{customerId}/checkout
- OrderController — GET /{id}, GET /number/{orderNumber}, GET /customer/{customerId}, PATCH /{id}/status, POST /{id}/cancel
- ReportController — GET /api/v1/reports/sales

Public routes (no authentication required):

- POST /api/v1/customers
- GET /api/v1/products, GET /api/v1/products/{id}, GET /api/v1/products/sku/{sku}
- GET /api/v1/categories, GET /api/v1/categories/{id}
- POST /api/v1/auth/login, POST /api/v1/auth/refresh

Private routes — CUSTOMER:

- Cart, Order (own), Customer (own)

Private routes — ADMIN:

- POST/PUT/DELETE products, POST categories, PATCH order status, GET reports

## 16. RabbitMQ Configuration

Located in infrastructure/config/RabbitMQConfig.java.
Declares all exchanges, queues and bindings as Spring beans.
RabbitAdmin creates them automatically in RabbitMQ on startup.

Exchanges:

- orderflow.orders (TopicExchange) — order lifecycle events
- orderflow.notifications (FanoutExchange) — email notifications
- orderflow.dlx (DirectExchange) — dead letter routing

Queues (all durable with x-dead-letter-exchange):

- order.created.queue → routing key: order.created
- order.paid.queue → routing key: order.paid
- order.shipped.queue → routing key: order.shipped
- order.cancelled.queue → routing key: order.cancelled
- email.notification.queue → fanout (no routing key)
- orderflow.dead-letter.queue → final destination for failed messages

Key Decisions:

- JacksonJsonMessageConverter used instead of deprecated Jackson2JsonMessageConverter — Spring AMQP 4.0 Jackson 3 support
- All queues configured with x-dead-letter-exchange — failed messages routed automatically to DLQ
- RabbitTemplate configured with JacksonJsonMessageConverter for automatic JSON serialization

## 17. Messaging

Located in infrastructure/adapter/output/messaging/ and infrastructure/adapter/input/messaging/.

Publisher:

- RabbitMQEventPublisher — implements EventPublisherPort, publishes events to orderflow.orders exchange via routing key
- OutboxEventScheduler — @Scheduled(fixedDelay = 5000), reads PENDING outbox events and publishes to RabbitMQ, marks as SENT or FAILED

Consumers (infrastructure/adapter/input/messaging/):

- OrderCreatedConsumer — reserves stock for each order item
- OrderPaidConsumer — confirms sale, decrements stockQuantity and reservedQuantity
- OrderCancelledConsumer — releases reserved stock
- OrderShippedConsumer — registers event as processed

All consumers check processed_event table before processing — idempotency guarantee.

## 18. Email Adapter

Located in infrastructure/adapter/output/email/MailEmailAdapter.java.
Implements EmailNotificationPort using JavaMailSender.
Sends emails via MailHog in development environment.

Methods:

- sendOrderConfirmation — sends confirmation email with order number and total
- sendOrderShipped — sends shipping email with tracking code
- sendOrderCancelled — sends cancellation email with reason

Key Decisions:

- Sender address externalised to application.yaml (app.mail.from) — no hardcoded values
- customerEmail added as snapshot field in ShopOrder — avoids extra repository call in email adapter
- SimpleMailMessage used — plain text emails sufficient for portfolio scope

## 19. Exception Handling

Located in infrastructure/config/handler/GlobalExceptionHandler.java.
Centralised exception handling via @RestControllerAdvice.

Exception mapping:

- ResourceNotFoundException → 404 Not Found
- BusinessRuleException → 422 Unprocessable Entity
- MethodArgumentNotValidException → 400 Bad Request (field errors joined)
- DataIntegrityViolationException → 409 Conflict
- Exception → 500 Internal Server Error

Error response format (ErrorResponse record):

- status: HTTP status code
- error: HTTP status reason phrase
- message: exception message
- path: request URI
- timestamp: LocalDateTime of occurrence

Key Decisions:

- GlobalExceptionHandler placed in infrastructure/config/handler/ — configuration concern, not a controller
- HttpStatus.UNPROCESSABLE_ENTITY deprecated in Spring 7.0 — replaced with status code 422 directly

## 20. OpenAPI Configuration

Located in infrastructure/config/OpenApiConfig.java.
Configures SpringDoc OpenAPI with project metadata.

Available at: http://localhost:8080/swagger-ui.html

Info:

- Title: order-flow API
- Version: 1.0.0
- Contact: Camila Kfouri (https://www.linkedin.com/in/camila-kfouri/)
- Server: http://localhost:8080 (Local Development)

## 21. Security Configuration

Located in infrastructure/config/security/.

Components:

- JwtService — generates and validates JWT tokens, extracts claims
- UserDetailsServiceImpl — implements UserDetailsService, loads user from database by email
- JwtAuthenticationFilter — intercepts every request, validates JWT and authenticates user in SecurityContext
- SecurityConfig — defines public/private routes, CORS, stateless session, JWT filter chain

Located in infrastructure/adapter/input/web/:

- AuthController — POST /api/v1/auth/login, POST /api/v1/auth/refresh

JWT Configuration (application.yaml):

- app.jwt.secret — signing key
- app.jwt.expiration — 900000ms (15 minutes)
- app.jwt.refresh-expiration — 604800000ms (7 days)

Token claims:

- sub: user email
- roles: list of granted authorities
- iat: issued at
- exp: expiration

Public routes:

- POST /api/v1/auth/login
- POST /api/v1/auth/refresh
- POST /api/v1/customers
- GET /api/v1/products/\*\*
- GET /api/v1/categories/\*\*
- /swagger-ui/**, /v3/api-docs/**

ADMIN only routes:

- POST/PUT/DELETE /api/v1/products/\*\*
- POST /api/v1/categories/\*\*
- PATCH /api/v1/orders/\*/status
- /api/v1/reports/\*\*

Key Decisions:

- JWT stateless — no server-side sessions
- BCryptPasswordEncoder for password hashing
- Refresh token rotated on every refresh — new access + refresh token issued
- DaoAuthenticationProvider configured with UserDetailsService constructor + setPasswordEncoder setter — Spring Security 7.0 API

## 22. Unit Tests

Located in src/test/java/com/camilagksantos/orderflow/.

Domain Tests:

- ShopOrderTest — 9 tests: state transitions, invalid transitions, cancellation
- ProductTest — 9 tests: stock reservation, release, confirmSale, activation
- CartTest — 8 tests: addItem, removeItem, total calculation, conversion
- CartItemTest — 2 tests: subtotal calculation
- CustomerTest — 2 tests: block, activate
- PaymentTest — 3 tests: approve, decline, attempt count
- MoneyTest — 8 tests: operations, validations, scale
- EmailTest — 6 tests: validation, lowercase conversion
- NIFTest — 6 tests: validation, check digit

Application Service Tests:

- CategoryServiceTest — 4 tests: create, find, findAll, not found
- CustomerServiceTest — 3 tests: register, find, not found
- CartServiceTest — 5 tests: add, create new cart, remove, find, not found
- ProductServiceTest — 6 tests: create, find, findAll, delete, not found
- OrderServiceTest — 8 tests: checkout, empty cart, duplicate key, find, cancel, update status
- PaymentServiceTest — 1 test: process payment

Application Mapper Tests:

- CategoryMapperTest — 2 tests: toResponse, toDomain
- ProductMapperTest — 2 tests: toResponse, toDomain
- CustomerMapperTest — 2 tests: toResponse, toDomain
- CartMapperTest — 2 tests: toResponse, toItemResponse
- OrderMapperTest — 1 test: toResponse

Total: 88 unit tests — all passing

Key Decisions:

- Mapper tests instantiate MapperImpl directly — no Spring context needed
- Mappers with @Autowired dependencies use reflection to inject collaborators
- NIF 123456789 used as valid test NIF — passes Portuguese check digit algorithm
- Mockito warnings with Java 26 are known and do not affect test results

## 23. Integration Tests

Located in src/test/java/com/camilagksantos/orderflow/.

Base class:

- BaseIntegrationTest — @SpringBootTest + @Testcontainers, spins up MySQL 8.0 and RabbitMQ containers via Testcontainers, registers dynamic properties for datasource and RabbitMQ

Tests:

- ProductFlowIntegrationTest — 5 tests: create category and product, find by id, find by sku, find all, reserve and release stock
- CustomerFlowIntegrationTest — 5 tests: register customer, find by id, find by email, find by nif, return empty when not found
- CartFlowIntegrationTest — 7 tests: create cart, create with items, calculate total, find by id, find active by customer id, remove item, convert

Key Decisions:

- @Transactional on test class — each test rolls back automatically, no state leakage between tests
- MySQL and RabbitMQ managed by Testcontainers — real containers, no mocks
- Dynamic properties registered via @DynamicPropertySource — datasource and RabbitMQ configured at runtime
- spring-boot-starter-flyway required in Spring Boot 4.x — Flyway no longer auto-configures without explicit starter
- baseline-on-migrate: true + baseline-version: 0 in application-test.yaml — handles empty schema on fresh container
- Each integration test persists a UserEntity via UserJpaRepository before creating a Customer — customer.user_id is a NOT NULL UNIQUE FK to the user table
- CartFlowIntegrationTest reuses a persistTestCustomer() helper chaining User -> Customer creation, same pattern as CustomerFlowIntegrationTest
- shouldRemoveItemFromCart validates orphanRemoval behavior on CartEntity.items, indirectly confirming the @AfterMapping back-reference fix works correctly
- OrderFlowIntegrationTest and PaymentFlowIntegrationTest reuse the persistTestCustomer() helper, extending the dependency chain to User -> Customer -> ShopOrder -> Payment
- PaymentEntity.id is a manually-generated UUID String (no @GeneratedValue), consistent with Cart, CartItem, ShopOrder, OrderItem

## In Progress

- Controller integration tests
- Excel report generation (Apache POI)
- Unit tests — frontend
- Integration tests — frontend (Cypress)

## Decisions Made During Implementation

- Domain models migrated from Java records to Lombok classes — records caused excessive MapStruct complexity due to behaviour methods being treated as mappable properties
- DTOs implemented as Java records — immutable transfer objects with no behaviour
- Value objects (Money, Email, NIF) remain as records — no behaviour methods that conflict with MapStruct
- NIF replaces CPF — Portuguese tax number with 9-digit check digit validation
- Address fields adapted for Portugal (district, postal_code, country default PT)
- Cart never expires — conscious product decision
- No address snapshot on order — address fetched from customer
- No stock movement audit table — stock controlled via product fields only
- No payment idempotency key — no real gateway integration
- Table names in singular following modern JPA convention
- shop_order used instead of order (reserved word in MySQL)
- Currency default EUR in Money value object
- Customer name, email and NIF are immutable after registration
- Port packages renamed from in/out to input/output — out is reserved by IntelliJ
- OrderItem does not use orphanRemoval — historical records must be preserved
- CartItem uses orphanRemoval — items do not exist outside their cart
- MapStruct replaced ModelMapper — compile-time mapping, type-safe, no runtime reflection
- Category moved to domain/category/ — independent package, not nested under product
- Type conversions in MapStruct via default methods — MapStruct detects automatically by signature, no @Named or expressions needed
- isDefault renamed to defaultAddress — avoids Lombok/MapStruct conflict with boolean isX() getter pattern
- OrderItem.subtotal added as real field — calculated on fromCart() and persisted, avoids expression in mapper
- Response DTOs use Money directly — richer JSON representation and easier log identification
- Request DTOs use BigDecimal for price — clients send simple numeric values
- ShopOrderPersistenceMapper inherits toMoney/toBigDecimal from OrderItemPersistenceMapper via uses — avoids ambiguous mapping methods
- customerEmail added as snapshot to ShopOrder — consistent with price snapshot pattern, avoids dependency on CustomerRepository in email adapter
- JacksonJsonMessageConverter replaces deprecated Jackson2JsonMessageConverter — Spring AMQP 4.0 Jackson 3 support
- app.mail.from externalised to application.yaml — no hardcoded values in adapters
- Product.confirmSale() added — decrements both stockQuantity and reservedQuantity on payment confirmation
- JWT stateless authentication — no server-side sessions, access token 15min, refresh token 7 days
- Refresh token rotated on every refresh — new pair issued on each refresh request
- DaoAuthenticationProvider configured via constructor (UserDetailsService) + setter (PasswordEncoder) — Spring Security 7.0 API
- spring-boot-starter-flyway required explicitly in Spring Boot 4.x — auto-configuration removed
- Testcontainers used for integration tests — MySQL and RabbitMQ real containers
- @Transactional on integration test classes — automatic rollback between tests
- Fixed CustomerPersistenceMapper bug where @Mapping(target = "user", ignore = true) silently persisted user_id as NULL despite Customer.userId being set in the domain object — replaced with explicit default conversion methods (Long <-> UserEntity), following the same pattern used for Money, Email and NIF
- Fixed the same FK-reference bug across AddressPersistenceMapper (via CustomerPersistenceMapper), ShopOrderPersistenceMapper, and PaymentPersistenceMapper — same root cause and same fix pattern as the Customer/Cart mappers (see Context Document 6.15)
- Fixed missing paymentMethod propagation in the checkout flow — CheckoutRequest, CheckoutUseCase, OrderService, and ShopOrder.fromCart() all lacked it, causing shop_order.payment_method (NOT NULL) to persist as null; fixed by adding it as a required parameter through the full chain from request DTO to domain factory (see Context Document 6.16)

## Known Issues / Blockers

None.
