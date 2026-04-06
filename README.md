# order-flow

> E-commerce REST API with hexagonal architecture, event-driven order processing via RabbitMQ, and async workflows.

---

## Overview

**order-flow** is a full-stack e-commerce portfolio project focused on backend engineering concepts that matter in production environments: clean architecture, asynchronous processing, and data consistency guarantees.

The project handles the complete order lifecycle — from product catalog and cart management to checkout, async payment processing, stock control, and email notifications — all driven by domain events through RabbitMQ.

---

## Tech Stack

### Backend
| Technology | Purpose |
|---|---|
| Java 21 + Spring Boot 3 | Core framework |
| MySQL 8 | Primary database |
| RabbitMQ | Message broker |
| Spring Data JPA | Persistence |
| Spring AMQP | Messaging |
| Flyway | Database migrations |
| Apache POI | Excel report generation |
| SpringDoc OpenAPI | API documentation |
| JUnit 5 + Mockito | Unit tests |
| Testcontainers | Integration tests |

### Frontend
| Technology | Purpose |
|---|---|
| Angular 17+ | SPA framework |
| Standalone Components | No NgModules |
| NgRx Signals Store | State management |
| Tailwind CSS | Styling |
| RxJS | Reactive programming |
| Jest | Unit tests |
| Cypress | E2E tests |

---

## Architecture

### Backend — Hexagonal (Ports & Adapters)

```
backend/
└── src/main/java/com/portfolio/orderflow/
    ├── domain/          # Aggregates, Value Objects, Domain Events
    ├── application/     # Use Cases (ports in/out), Services
    └── infrastructure/ # REST Controllers, JPA Adapters, RabbitMQ, Email
```

The domain layer has zero dependencies on frameworks or infrastructure — business rules live entirely inside aggregates and domain services.

### Order State Machine

```
PENDING → PAID → PREPARING → SHIPPED → DELIVERED
   ↓         ↓         ↓
CANCELLED  REFUNDED  CANCELLED
```

### Event-Driven Flow

```
POST /checkout
  └── creates Order (PENDING)
  └── persists OutboxEvent
        └── Scheduler publishes to RabbitMQ
              ├── StockConsumer    → reserves stock
              ├── EmailConsumer    → sends confirmation
              └── StatusConsumer   → updates order status
```

Key guarantees:
- **Transactional Outbox Pattern** — event publishing is atomic with the database write
- **Idempotency** — each event carries a unique key; duplicate messages are safely ignored
- **Dead Letter Queue** — failed messages are routed to DLQ for inspection and replay

---

## Domain Model

| Aggregate | Responsibilities |
|---|---|
| `Product` | Catalog, pricing, stock quantity + reserved quantity |
| `Customer` | Registration, addresses, CPF/email validation |
| `Cart` | Items with price snapshots, expiration, guest support |
| `Order` | Full lifecycle, item snapshots, status transitions |
| `Payment` | Gateway integration, retry control, idempotency |

> Cart items and order items store **snapshots** of product name, SKU, and price at the time of the action — price changes never affect historical records.

---

## Getting Started

### Prerequisites
- Docker and Docker Compose
- Java 21
- Node.js 20+

### Running locally

```bash
# Clone the repository
git clone https://github.com/your-username/order-flow.git
cd order-flow

# Start infrastructure (MySQL, RabbitMQ, MailHog)
docker-compose up -d

# Run the backend
cd backend
./mvnw spring-boot:run

# Run the frontend
cd frontend
npm install
ng serve
```

### Access points

| Service | URL |
|---|---|
| Frontend | http://localhost:4200 |
| API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| RabbitMQ Management | http://localhost:15672 |
| MailHog (email preview) | http://localhost:8025 |

---

## API Documentation

Full API documentation is available via Swagger UI at `/swagger-ui.html` when the backend is running.

Main resource groups:
- `POST /api/v1/carts/{id}/checkout` — initiates async order processing
- `GET /api/v1/orders/{id}` — order details and current status
- `GET /api/v1/reports/sales?startDate=&endDate=&format=xlsx` — Excel sales report

---

## Project Structure

```
order-flow/
├── backend/           # Spring Boot application
├── frontend/          # Angular application
├── docs-project/      # Architecture decisions and diagrams
├── docker-compose.yml
└── README.md
```

---

## Author

Made by **Camila Kfouri**

[![GitHub](https://img.shields.io/badge/GitHub-camilagksantos-181717?style=flat&logo=github)](https://github.com/camilagksantos)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-camila--kfouri-0A66C2?style=flat&logo=linkedin)](https://www.linkedin.com/in/camila-kfouri/)
