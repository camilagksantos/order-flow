package com.camilagksantos.orderflow.domain.event;

import java.time.LocalDateTime;

public record OrderStatusChangedEvent(
        String eventId,
        String aggregateId,
        String previousStatus,
        String newStatus,
        LocalDateTime occurredAt
) implements DomainEvent {

    @Override
    public String eventType() {
        return "ORDER_STATUS_CHANGED";
    }
}