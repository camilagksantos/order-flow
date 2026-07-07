package com.camilagksantos.orderflow.domain.event;

import java.time.LocalDateTime;

public interface DomainEvent {
    String eventId();
    String eventType();
    String aggregateId();
    LocalDateTime occurredAt();
}
