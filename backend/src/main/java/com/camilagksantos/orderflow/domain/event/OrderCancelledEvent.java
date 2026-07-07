package com.camilagksantos.orderflow.domain.event;

import java.time.LocalDateTime;

public record OrderCancelledEvent(
        String eventId,
        String aggregateId,
        String reason,
        LocalDateTime occurredAt
) implements DomainEvent {

    @Override
    public String eventType() {
        return "ORDER_CANCELLED";
    }
}
