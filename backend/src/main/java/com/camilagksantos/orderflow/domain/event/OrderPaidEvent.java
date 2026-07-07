package com.camilagksantos.orderflow.domain.event;

import java.time.LocalDateTime;

public record OrderPaidEvent(
        String eventId,
        String aggregateId,
        LocalDateTime occurredAt
) implements DomainEvent {

    @Override
    public String eventType() {
        return "ORDER_PAID";
    }
}
