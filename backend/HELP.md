---

## 🛢 Database Migrations

Managed with Flyway.

Location: `backend/src/main/resources/db/migration/`

| Migration | Description |
|---|---|
| V1 | Create categories table |
| V2 | Create products table |
| V3 | Create customers and addresses tables |
| V4 | Create carts and cart_items tables |
| V5 | Create orders and order_items tables |
| V6 | Create payments table |
| V7 | Create stock_movements table |
| V8 | Create outbox_events table |
| V9 | Create processed_events table |

---

## 🔐 Security

Authentication and authorization are handled by Spring Security.

Role-based access:
- `CUSTOMER` — manage own cart, place and view own orders
- `ADMIN` — manage products, update order status, access reports

---

## 🛠 Tech Stack

### Backend

| Category | Technology |
|---|---|
| Framework | Spring Boot 4.0.5 |
| Language | Java 26 |
| Persistence | Spring Data JPA |
| Database | MySQL 8 |
| Migrations | Flyway |
| Messaging | RabbitMQ + Spring AMQP |
| Mapping | ModelMapper |
| Reporting | Apache POI |
| Boilerplate | Lombok |
| Documentation | SpringDoc OpenAPI |
| Testing | JUnit 5 + Mockito + Testcontainers |
| Build | Maven |

### Frontend

| Category | Technology |
|---|---|
| Framework | Angular (Standalone Components) |
| State Management | NgRx Signals Store |
| Styling | Tailwind CSS |
| Reactive | RxJS |
| Unit Tests | Jest |
| E2E Tests | Cypress |

---

## ⚙️ System Design Decisions

**Why Hexagonal Architecture instead of traditional layered MVC?**
Traditional MVC couples business logic directly to the framework. With Hexagonal Architecture the domain has zero dependencies on Spring, JPA or any external library — it is plain Java. Business rules can be tested in complete isolation and the infrastructure can be swapped without touching the domain. The separation between domain model and JPA entity, while more verbose, is intentional: it enforces this boundary at compile time.

**Why the Transactional Outbox Pattern instead of publishing directly to RabbitMQ?**
Publishing an event directly to RabbitMQ inside a database transaction creates a distributed consistency problem: if the broker is unavailable or the application crashes after the commit but before the publish, the event is lost. The Outbox Pattern persists the event in the same database transaction as the order. A scheduler then reads pending events and publishes them to the broker, guaranteeing at-least-once delivery without distributed transactions.

**Why snapshots on cart and order items?**
Product prices change over time. If order items referenced the live product directly, a price update would silently alter the historical value of every past order. Storing snapshots of name, SKU, and price at the moment of the action preserves data integrity and makes every order a self-contained, auditable record.

**Why an explicit `idempotencyKey` on Order and Payment?**
In an event-driven system with at-least-once delivery, the same message may be processed more than once — due to retries, network issues or broker redelivery. The `idempotencyKey` allows each consumer to check whether the event was already processed before doing any work, making all consumers safe to retry without side effects.

**Why a Dead Letter Queue?**
Without a DLQ, a message that consistently fails to process blocks the queue or is silently dropped. Routing failed messages to a dedicated dead letter queue allows the team to inspect, diagnose and replay them without data loss or manual intervention in the main processing flow.

**Why Flyway for migrations instead of `spring.jpa.hibernate.ddl-auto`?**
`ddl-auto` is convenient in development but unpredictable in production — it can silently alter or drop schema. Flyway applies versioned, explicit SQL scripts in a controlled order. Every schema change is tracked, reproducible and reviewable in version control.

---

## 🔭 Future Improvements

- CI/CD pipeline with GitHub Actions
- Coverage report with JaCoCo
- Rate limiting on public endpoints
- Payment gateway integration (Stripe or Mercado Pago)
- Push notifications for order status updates

---

## 👩‍💻 Author

Made by **Camila Kfouri**

[![GitHub](https://img.shields.io/badge/GitHub-camilagksantos-181717?style=flat&logo=github)](https://github.com/camilagksantos)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-camila--kfouri-0A66C2?style=flat&logo=linkedin)](https://www.linkedin.com/in/camila-kfouri/)

*This project is part of a portfolio series focused on modern backend architecture and real-world practices.*