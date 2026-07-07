package com.camilagksantos.orderflow.application.port.output;

import com.camilagksantos.orderflow.domain.event.DomainEvent;

public interface EventPublisherPort {

    void publish(DomainEvent event);
}
