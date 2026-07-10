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

Mappers:

- RolePersistenceMapper — Role ↔ RoleEntity (direct field mapping)
- UserPersistenceMapper — User ↔ UserEntity (uses RolePersistenceMapper)
- CategoryPersistenceMapper — Category ↔ CategoryEntity (direct field mapping)
- ProductPersistenceMapper — Product ↔ ProductEntity (Money expression, uses CategoryPersistenceMapper)
- AddressPersistenceMapper — Address ↔ AddressEntity (customer.id → customerId)
- CustomerPersistenceMapper — Customer ↔ CustomerEntity (Email/NIF expressions, uses AddressPersistenceMapper)
- CartItemPersistenceMapper — CartItem ↔ CartItemEntity (Money expression, cart.id → cartId)
- CartPersistenceMapper — Cart ↔ CartEntity (customer.id → customerId, uses CartItemPersistenceMapper)
- OrderItemPersistenceMapper — OrderItem ↔ OrderItemEntity (Money expression, order.id → orderId)
- ShopOrderPersistenceMapper — ShopOrder ↔ ShopOrderEntity (Money expressions, uses OrderItemPersistenceMapper)
- PaymentPersistenceMapper — Payment ↔ PaymentEntity (Money expression, order.id → orderId)
- OutboxEventPersistenceMapper — OutboxEvent ↔ OutboxEventEntity (direct mapping)

Key Decisions:

- createdAt and updatedAt ignored in toEntity() — managed by @PrePersist / @PreUpdate
- Relationship fields (customer, cart, order) ignored in toEntity() — set by JPA cascade
- Money ↔ BigDecimal conversion via expression: Money.of(BigDecimal) / money.amount()
- Email ↔ String conversion via expression: new Email(String) / email.value()
- NIF ↔ String conversion via expression: new NIF(String) / nif.value()

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

## 13. In Progress

- Application mappers (DTO ↔ Domain)
- DTOs (request / response — Java records)
- REST controllers
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

## 14. Decisions Made During Implementation

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

## 15. Known Issues / Blockers

None.
