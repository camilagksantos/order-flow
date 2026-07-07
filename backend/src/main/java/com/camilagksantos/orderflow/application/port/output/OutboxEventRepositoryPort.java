package com.camilagksantos.orderflow.application.port.output;

import com.camilagksantos.orderflow.domain.event.OutboxEvent;
import com.camilagksantos.orderflow.domain.event.OutboxEventStatus;

import java.util.List;

public interface OutboxEventRepositoryPort {

    OutboxEvent save(OutboxEvent event);
    List<OutboxEvent> findByStatus(OutboxEventStatus status);
    OutboxEvent updateStatus(String id, OutboxEventStatus status);
}
