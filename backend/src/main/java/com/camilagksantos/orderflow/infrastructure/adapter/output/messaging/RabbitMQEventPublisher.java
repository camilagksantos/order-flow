package com.camilagksantos.orderflow.infrastructure.adapter.output.messaging;

import com.camilagksantos.orderflow.application.port.output.EventPublisherPort;
import com.camilagksantos.orderflow.domain.event.DomainEvent;
import com.camilagksantos.orderflow.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RabbitMQEventPublisher implements EventPublisherPort {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(DomainEvent event) {
        String routingKey = switch (event.eventType()) {
            case "ORDER_CREATED" -> RabbitMQConfig.ORDER_CREATED_KEY;
            case "ORDER_PAID" -> RabbitMQConfig.ORDER_PAID_KEY;
            case "ORDER_SHIPPED" -> RabbitMQConfig.ORDER_SHIPPED_KEY;
            case "ORDER_CANCELLED" -> RabbitMQConfig.ORDER_CANCELLED_KEY;
            default -> throw new IllegalArgumentException("Unknown event type: " + event.eventType());
        };
        rabbitTemplate.convertAndSend(RabbitMQConfig.ORDERS_EXCHANGE, routingKey, event);
    }
}