package com.camilagksantos.orderflow.infrastructure.adapter.input.messaging;

import com.camilagksantos.orderflow.application.port.output.ProcessedEventRepositoryPort;
import com.camilagksantos.orderflow.domain.event.OutboxEvent;
import com.camilagksantos.orderflow.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderShippedConsumer {

    private final ProcessedEventRepositoryPort processedEventRepositoryPort;

    @RabbitListener(queues = RabbitMQConfig.ORDER_SHIPPED_QUEUE)
    public void consume(OutboxEvent event) {
        if (processedEventRepositoryPort.existsById(event.id())) {
            log.warn("Duplicate event ignored: {}", event.id());
            return;
        }

        processedEventRepositoryPort.save(event.id());
        log.info("Order shipped event processed: {}", event.id());
    }
}