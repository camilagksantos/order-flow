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
│   ├── auth/            ← User, Role
│   ├── cart/            ← Cart, CartItem, CartStatus
│   ├── customer/        ← Customer, Address, CustomerStatus
│   ├── event/           ← DomainEvent, domain events, OutboxEvent
│   ├── exception/       ← domain exceptions
│   ├── order/           ← ShopOrder, OrderItem, OrderStatus, PaymentMethod
│   ├── payment/         ← Payment, PaymentStatus
│   ├── product/         ← Product, Category, ProductStatus
│   └── shared/          ← Money, Email, NIF
├── application/
│   ├── port/
│   │   ├── input/       ← use case interfaces
│   │   └── output/      ← repository and event interfaces
│   ├── service/         ← use case implementations
│   ├── dto/             ← request / response models
│   └── mapper/          ← domain ↔ dto mappers
└── infrastructure/
├── adapter/
│   ├── input/
│   │   ├── web/     ← REST controllers
│   │   └── messaging/ ← RabbitMQ consumers
│   └── output/
│       ├── persistence/ ← JPA adapters
│       ├── messaging/   ← RabbitMQ publisher
│       └── email/       ← MailHog adapter
├── persistence/
│   ├── entity/      ← JPA entities
│   ├── repository/  ← Spring Data JPA interfaces
│   └── mapper/      ← domain ↔ entity mappers
└── config/          ← Spring beans, RabbitMQ, OpenAPI, Security

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

### 6.2 Domain Models as Records
All domain models are implemented as Java records.

Benefits:
- Immutability by default
- Thread safety
- Value semantics
- No boilerplate

Records cannot be used as JPA entities — a separate JPA entity class
exists for each aggregate in the infrastructure layer.

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

ModelMapper is used for straightforward mappings.
Manual mapping is used where custom logic is required.

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
- ModelMapper
- Apache POI
- Lombok
- SpringDoc OpenAPI

### Testing
- JUnit 5
- Mockito
- Testcontainers (MySQL + RabbitMQ)

## 9. Testing Strategy

| Layer | Type | Tool | Target |
|---|---|---|---|
| Domain models | Unit | JUnit 5 | 90%+ |
| Application services | Unit | JUnit 5 + Mockito | 85%+ |
| Controllers | Integration | @SpringBootTest | 80%+ |
| RabbitMQ consumers | Integration | Testcontainers | 80%+ |
| Repositories | Integration | Testcontainers | 80%+ |

## 10. Database Indexes

| Table | Index | Purpose |
|---|---|---|
| product | category_id | category filtering |
| product | status | active product queries |
| shop_order | customer_id | customer order history |
| shop_order | status | order management queries |
| shop_order | idempotency_key | duplicate prevention |
| cart | customer_id | customer cart lookup |
| outbox_event | status | pending event polling |