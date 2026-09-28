package com.camilagksantos.orderflow.infrastructure.adapter.input.messaging;

import com.camilagksantos.orderflow.application.port.output.EmailNotificationPort;
import com.camilagksantos.orderflow.application.port.output.OrderRepositoryPort;
import com.camilagksantos.orderflow.application.port.output.ProcessedEventRepositoryPort;
import com.camilagksantos.orderflow.domain.event.OutboxEvent;
import com.camilagksantos.orderflow.domain.order.ShopOrder;
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
    private final OrderRepositoryPort orderRepositoryPort;
    private final EmailNotificationPort emailNotificationPort;

    @RabbitListener(queues = RabbitMQConfig.ORDER_SHIPPED_QUEUE)
    public void consume(OutboxEvent event) {
        if (processedEventRepositoryPort.existsById(event.id())) {
            log.warn("Duplicate event ignored: {}", event.id());
            return;
        }

        try {
            ShopOrder order = orderRepositoryPort.findById(event.payload())
                    .orElseThrow(() -> new RuntimeException("Order not found: " + event.payload()));

            processedEventRepositoryPort.save(event.id());

            try {
                emailNotificationPort.sendOrderShipped(order);
            } catch (Exception e) {
                log.error("Failed to send order shipped email: {}", event.id(), e);
            }

            log.info("Order shipped event processed: {}", event.id());
        } catch (Exception e) {
            log.error("Failed to process order shipped event: {}", event.id(), e);
            throw e;
        }
    }
}