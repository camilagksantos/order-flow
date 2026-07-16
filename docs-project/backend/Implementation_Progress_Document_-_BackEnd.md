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

## 2. Database Migrations

Files:

- V1\_\_create_schema.sql

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

- Product — reserve/release/activate/deactivate (void mutations)
- Customer — block/activate (void mutations)
- Cart — addItem/removeItem/convert (void mutations) + newCart (static factory)
- ShopOrder — pay/startPreparing/ship/deliver/cancel (void mutations) + fromCart (static factory)
- Payment — approve/decline (void mutations)

Supporting Entities:

- Category (domain/category/)
- Address (domain/customer/)
- CartItem — stores price snapshot at time of addition
- OrderItem — stores price snapshot at time of checkout
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
- CheckoutRequest — idempotencyKey, addressId
- UpdateOrderStatusRequest — status
- CancelOrderRequest — reason
- ProcessPaymentRequest — orderId, method, cardLastFour, cardBrand, mbwayPhone, mbEntity, mbReference
- LoginRequest — email, password

Response DTOs (application/dto/response/):

- CategoryResponse — id, name
- ProductResponse — id, name, description, sku, price, stockQuantity, reservedQuantity, availableQuantity, category, imageUrl, status
- AddressResponse — id, street, number, complement, neighborhood, city, district, postalCode, country, isDefault
- CustomerResponse — id, name, email, nif, phone, status, addresses
- CartItemResponse — id, productId, productName, productSku, unitPrice, quantity, subtotal
- CartResponse — id, customerId, status, items, total
- OrderItemResponse — id, productId, productName, productSku, unitPrice, quantity, subtotal
- OrderResponse — id, orderNumber, customerId, status, items, subtotal, shippingCost, discountAmount, totalAmount, paymentMethod, trackingCode, timestamps
- PaymentResponse — id, orderId, amount, method, status, transactionId, processedAt, createdAt
- TokenResponse — accessToken, refreshToken, tokenType, expiresIn
- ErrorResponse — status, error, message, path, timestamp

Key Decision:

- One DTO class per operation where fields differ (CreateProduct vs UpdateProduct — sku immutable)
- Single DTO class when fields are identical across operations

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

- Response DTOs use Money directly — richer JSON representation and easier log identification
- Request DTOs use BigDecimal for price — clients send simple numeric values
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

## 16. In Progress

- RabbitMQ configuration
- Messaging consumers
- Email adapter
- Outbox event scheduler
- Exception handling (@ControllerAdvice)
- Security configuration (JWT)
- OpenAPI configuration
- Excel report generation (Apache POI)
- Unit tests
- Integration tests

## 17. Decisions Made During Implementation

- Domain models migrated from Java records to Lombok classes — records caused excessive MapStruct complexity due to behaviour methods being treated as mappable properties
- DTOs will remain as Java records — immutable transfer objects with no behaviour
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

## 18. Known Issues / Blockers

None.
