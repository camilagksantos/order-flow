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
- Order
- Payment

### Value Objects
- Money (amount + currency)
- Email (validated)
- CPF (validated with check digit)
- Dimensions (width, height, depth)
- CustomerSnapshot (name, email, cpf, phone)
- PaymentDetails (card, pix or boleto data)

### Supporting Entities
- Category
- ProductImage
- Address
- CartItem
- OrderItem
- StockMovement
- OutboxEvent

### Domain Events
- OrderCreatedEvent
- OrderPaidEvent
- OrderCancelledEvent
- OrderShippedEvent
- OrderStatusChangedEvent
- StockReservedEvent
- StockReservationFailedEvent

## 3. Order State Machine

Allowed transitions:

- PENDING → PAID, CANCELLED
- PAID → PREPARING, REFUNDED
- PREPARING → SHIPPED, CANCELLED
- SHIPPED → DELIVERED
- DELIVERED → REFUNDED

## 4. Environment Profiles

- dev: local development with Docker (MySQL, RabbitMQ, MailHog)
- test: Testcontainers (spins up real containers per test suite)
- prod: production-ready configuration via environment variables

## 5. Key Architecture Decisions

### 5.1 Domain Model vs JPA Entity separation
Domain models are plain Java records — immutable, framework-free.
JPA entities live exclusively in the infrastructure layer.
Mappers handle conversion between the two.
This enforces the hexagonal boundary at compile time.

### 5.2 Transactional Outbox Pattern
Events are persisted in the same database transaction as the aggregate.
A scheduler reads pending OutboxEvents and publishes them to RabbitMQ.
Guarantees at-least-once delivery without distributed transactions.

### 5.3 Idempotency
Order and Payment carry an idempotencyKey (UUID).
Each consumer checks the processed_events table before handling a message.
Duplicate messages are safely ignored without side effects.

### 5.4 Price Snapshots
CartItem and OrderItem store snapshots of product name, SKU and price
at the moment of the action.
Price changes never affect historical cart or order records.

### 5.5 Stock Control
Product has two fields: stockQuantity and reservedQuantity.
availableQuantity = stockQuantity - reservedQuantity.
Reservation happens asynchronously via StockConsumer after order creation.
Release happens via StockReleaseConsumer on cancellation.

### 5.6 Dead Letter Queue
Failed messages are routed to ecommerce.dlx exchange.
Allows inspection, diagnosis and replay without data loss.

### 5.7 Exception Handling
Centralised via @ControllerAdvice.

Exception mapping:
- 404: ResourceNotFoundException
- 422: BusinessRuleException
- 400: MethodArgumentNotValidException
- 409: DataIntegrityViolationException
- 500: Generic Exception

### 5.8 CORS Configuration
- Allowed origin: http://localhost:4200
- Methods: GET, POST, PUT, PATCH, DELETE, OPTIONS

## 6. RabbitMQ Configuration

### Exchanges
- ecommerce.orders (topic)
- ecommerce.stock (direct)
- ecommerce.notifications (fanout)
- ecommerce.dlx (dead letter)

### Queues
- order.created.queue
- order.paid.queue
- order.shipped.queue
- order.cancelled.queue
- stock.reserve.queue
- stock.release.queue
- email.notification.queue
- ecommerce.dead-letter.queue

## 7. Technology Stack

### Backend
- Java 26
- Spring Boot 4.0.5
- Spring Web
- Spring Data JPA
- Spring AMQP (RabbitMQ)
- Spring Security
- MySQL 8
- Flyway
- ModelMapper
- Apache POI
- Lombok
- SpringDoc OpenAPI

### Testing
- JUnit 5
- Mockito
- Testcontainers

## 8. Testing Strategy

| Layer | Type | Tool | Target |
|---|---|---|---|
| Domain services | Unit | JUnit 5 + Mockito | 90%+ |
| Use cases | Unit | JUnit 5 + Mockito | 85%+ |
| Controllers | Integration | @SpringBootTest | 80%+ |
| RabbitMQ consumers | Integration | Testcontainers | 80%+ |
| Repositories | Integration | Testcontainers | 80%+ |

## 9. Next Steps

- Add ModelMapper and Apache POI to pom.xml
- Create package structure (hexagonal)
- Configure application profiles
- Implement Flyway migrations
- Implement domain models
- Implement JPA entities
- Implement ports (in and out)
- Implement application services
- Implement REST controllers
- Implement RabbitMQ consumers
- Implement exception handling
- Implement tests
