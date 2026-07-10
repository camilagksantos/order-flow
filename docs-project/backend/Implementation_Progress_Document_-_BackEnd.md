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

All implemented as Java records — immutable and framework-independent.

Value Objects (domain/shared/):

- Money — amount (BigDecimal) + currency (String, default EUR), with add/subtract/multiply operations
- Email — validated by regex, lowercase enforced on construction
- NIF — 9 digits, check digit validated using Portuguese algorithm

Aggregates:

- Product — with reserve/release/activate/deactivate behaviour
- Customer — with block/activate behaviour
- Cart — with addItem/removeItem/convert behaviour and newCart factory method
- ShopOrder — with full state machine (pay/startPreparing/ship/deliver/cancel) and fromCart factory method
- Payment — with approve/decline behaviour

Supporting Entities:

- Category (domain/product/)
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

- User — with activate/deactivate behaviour and hasRole check
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

Key Decision:

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

## 11. In Progress

- Persistence adapters
- Persistence mappers
- Application mappers
- DTOs
- REST controllers
- RabbitMQ configuration
- Messaging consumers
- Email adapter
- Outbox event scheduler
- Exception handling (@ControllerAdvice)
- Security configuration
- OpenAPI configuration
- Excel report generation
- Unit tests
- Integration tests

## 12. Decisions Made During Implementation

- All domain models implemented as Java records (immutable, framework-free)
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
- ModelMapper replaced with MapStruct — compile-time mapping, type-safe, no runtime reflection

## 13. Known Issues / Blockers

- Spring cannot autowire output ports until persistence adapters are implemented — expected, not an error
