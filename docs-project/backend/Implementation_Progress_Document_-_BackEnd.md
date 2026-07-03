# order-flow — Implementation Progress Document (Backend)

## 1. Implemented Areas

- [x] pom.xml dependencies (ModelMapper, Apache POI, Spring Mail)
- [x] Package structure (hexagonal)
- [x] Application profiles configuration
- [x] Database migrations (Flyway — V1 full schema)
- [x] Value Objects (Money, Email, NIF)
- [ ] Domain models (aggregates and entities)
- [ ] Domain exceptions
- [ ] JPA entities
- [ ] Input ports (use case interfaces)
- [ ] Output ports (repository interfaces)
- [ ] Application services
- [ ] JPA repositories
- [ ] Persistence adapters
- [ ] RabbitMQ configuration
- [ ] Messaging consumers
- [ ] Email adapter
- [ ] Outbox event scheduler
- [ ] DTOs (request / response)
- [ ] Mappers (domain ↔ dto, domain ↔ entity)
- [ ] REST controllers
- [ ] Exception handling (@ControllerAdvice)
- [ ] Security configuration
- [ ] OpenAPI configuration
- [ ] Excel report generation (Apache POI)
- [ ] Unit tests
- [ ] Integration tests

## 2. In Progress

- Domain models (aggregates and entities)

## 3. Decisions Made During Implementation

- All domain models implemented as Java records (immutable, framework-free)
- Removed CPF in favour of NIF (Portuguese tax number)
- Removed price snapshots on order address — address fetched from customer
- Removed stock movement audit table — stock control via product fields only
- Removed payment idempotency key — no real gateway integration
- Cart expiration removed — conscious product decision
- Table names in singular following modern JPA convention
- shop_order used instead of order (reserved word in MySQL)
- Currency default set to EUR
- Customer name, email and NIF are immutable after registration

## 4. Known Issues / Blockers

(none)