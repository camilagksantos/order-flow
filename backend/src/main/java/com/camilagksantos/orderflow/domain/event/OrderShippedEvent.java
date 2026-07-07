package com.camilagksantos.orderflow.domain.event;

import java.time.LocalDateTime;

public record OrderShippedEvent(
        String eventId,
        String aggregateId,
        String trackingCode,
        LocalDateTime occurredAt
) implements DomainEvent {

    @Override
    public String eventType() {
        return "ORDER_SHIPPED";
    }
}