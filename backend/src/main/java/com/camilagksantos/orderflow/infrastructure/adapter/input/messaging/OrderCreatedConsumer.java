package com.camilagksantos.orderflow.infrastructure.adapter.input.messaging;

import com.camilagksantos.orderflow.application.port.output.ProcessedEventRepositoryPort;
import com.camilagksantos.orderflow.application.port.output.ProductRepositoryPort;
import com.camilagksantos.orderflow.application.port.output.OrderRepositoryPort;
import com.camilagksantos.orderflow.domain.order.ShopOrder;
import com.camilagksantos.orderflow.domain.event.OutboxEvent;
import com.camilagksantos.orderflow.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedConsumer {

    private final OrderRepositoryPort orderRepositoryPort;
    private final ProductRepositoryPort productRepositoryPort;
    private final ProcessedEventRepositoryPort processedEventRepositoryPort;

    @RabbitListener(queues = RabbitMQConfig.ORDER_CREATED_QUEUE)
    public void consume(OutboxEvent event) {
        if (processedEventRepositoryPort.existsById(event.id())) {
            log.warn("Duplicate event ignored: {}", event.id());
            return;
        }

        try {
            ShopOrder order = orderRepositoryPort.findById(event.payload())
                    .orElseThrow(() -> new RuntimeException("Order not found: " + event.payload()));

            order.getItems().forEach(item ->
                    productRepositoryPort.findById(item.getProductId()).ifPresent(product -> {
                        product.reserve(item.getQuantity());
                        productRepositoryPort.save(product);
                    })
            );

            processedEventRepositoryPort.save(event.id());
            log.info("Order created event processed: {}", event.id());
        } catch (Exception e) {
            log.error("Failed to process order created event: {}", event.id(), e);
            throw e;
        }
    }
}