package com.camilagksantos.orderflow.infrastructure.adapter.output.messaging;

import com.camilagksantos.orderflow.application.port.output.OutboxEventRepositoryPort;
import com.camilagksantos.orderflow.domain.event.OutboxEvent;
import com.camilagksantos.orderflow.domain.event.OutboxEventStatus;
import com.camilagksantos.orderflow.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventScheduler {

    private final OutboxEventRepositoryPort outboxEventRepositoryPort;
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedDelay = 5000)
    public void processOutboxEvents() {
        outboxEventRepositoryPort.findByStatus(OutboxEventStatus.PENDING)
                .forEach(this::processEvent);
    }

    private void processEvent(OutboxEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ORDERS_EXCHANGE,
                    resolveRoutingKey(event.eventType()),
                    event
            );
            outboxEventRepositoryPort.updateStatus(event.id(), OutboxEventStatus.SENT);
            log.info("Outbox event published: {} - {}", event.eventType(), event.id());
        } catch (Exception e) {
            outboxEventRepositoryPort.updateStatus(event.id(), OutboxEventStatus.FAILED);
            log.error("Failed to publish outbox event: {} - {}", event.eventType(), event.id(), e);
        }
    }

    private String resolveRoutingKey(String eventType) {
        return switch (eventType) {
            case "ORDER_CREATED" -> RabbitMQConfig.ORDER_CREATED_KEY;
            case "ORDER_PAID" -> RabbitMQConfig.ORDER_PAID_KEY;
            case "ORDER_SHIPPED" -> RabbitMQConfig.ORDER_SHIPPED_KEY;
            case "ORDER_CANCELLED" -> RabbitMQConfig.ORDER_CANCELLED_KEY;
            default -> throw new IllegalArgumentException("Unknown event type: " + eventType);
        };
    }
}