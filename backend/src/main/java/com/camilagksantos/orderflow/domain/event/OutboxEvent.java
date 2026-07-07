package com.camilagksantos.orderflow.domain.event;

import java.time.LocalDateTime;

public record OutboxEvent(
        String id,
        String eventType,
        String payload,
        OutboxEventStatus status,
        LocalDateTime createdAt
) {

    public OutboxEvent {
        if (eventType == null || eventType.isBlank()) throw new IllegalArgumentException("Event type must not be blank");
        if (payload == null || payload.isBlank()) throw new IllegalArgumentException("Payload must not be blank");
        if (status == null) throw new IllegalArgumentException("Status must not be null");
    }
}
