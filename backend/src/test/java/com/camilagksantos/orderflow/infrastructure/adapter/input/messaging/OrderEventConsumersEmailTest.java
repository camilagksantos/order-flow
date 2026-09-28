package com.camilagksantos.orderflow.infrastructure.adapter.input.messaging;

import com.camilagksantos.orderflow.application.port.output.EmailNotificationPort;
import com.camilagksantos.orderflow.application.port.output.OrderRepositoryPort;
import com.camilagksantos.orderflow.application.port.output.ProcessedEventRepositoryPort;
import com.camilagksantos.orderflow.application.port.output.ProductRepositoryPort;
import com.camilagksantos.orderflow.domain.event.OutboxEvent;
import com.camilagksantos.orderflow.domain.event.OutboxEventStatus;
import com.camilagksantos.orderflow.domain.order.OrderStatus;
import com.camilagksantos.orderflow.domain.order.ShopOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderEventConsumersEmailTest {

    @Mock
    private OrderRepositoryPort orderRepositoryPort;

    @Mock
    private ProductRepositoryPort productRepositoryPort;

    @Mock
    private ProcessedEventRepositoryPort processedEventRepositoryPort;

    @Mock
    private EmailNotificationPort emailNotificationPort;

    private ShopOrder order;

    @BeforeEach
    void setUp() {
        order = ShopOrder.builder()
                .id(UUID.randomUUID().toString())
                .orderNumber("ORD-TEST-001")
                .customerId(1L)
                .customerEmail("camila@test.com")
                .status(OrderStatus.PENDING)
                .items(List.of())
                .build();
    }

    private OutboxEvent eventFor(String eventType) {
        return new OutboxEvent(UUID.randomUUID().toString(), eventType, order.getId(),
                OutboxEventStatus.PENDING, LocalDateTime.now());
    }

    @Test
    void shouldSendConfirmationEmailWhenOrderCreatedProcessed() {
        OrderCreatedConsumer consumer = new OrderCreatedConsumer(
                orderRepositoryPort, productRepositoryPort, processedEventRepositoryPort, emailNotificationPort);
        OutboxEvent event = eventFor("ORDER_CREATED");
        when(orderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));

        consumer.consume(event);

        verify(emailNotificationPort).sendOrderConfirmation(order);
        verify(processedEventRepositoryPort).save(event.id());
    }

    @Test
    void shouldSendCancelledEmailWhenOrderCancelledProcessed() {
        OrderCancelledConsumer consumer = new OrderCancelledConsumer(
                orderRepositoryPort, productRepositoryPort, processedEventRepositoryPort, emailNotificationPort);
        OutboxEvent event = eventFor("ORDER_CANCELLED");
        when(orderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));

        consumer.consume(event);

        verify(emailNotificationPort).sendOrderCancelled(order);
        verify(processedEventRepositoryPort).save(event.id());
    }

    @Test
    void shouldSendShippedEmailWhenOrderShippedProcessed() {
        OrderShippedConsumer consumer = new OrderShippedConsumer(
                processedEventRepositoryPort, orderRepositoryPort, emailNotificationPort);
        OutboxEvent event = eventFor("ORDER_SHIPPED");
        when(orderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));

        consumer.consume(event);

        verify(emailNotificationPort).sendOrderShipped(order);
        verify(processedEventRepositoryPort).save(event.id());
    }

    @Test
    void shouldStillMarkEventProcessedWhenEmailFails() {
        OrderShippedConsumer consumer = new OrderShippedConsumer(
                processedEventRepositoryPort, orderRepositoryPort, emailNotificationPort);
        OutboxEvent event = eventFor("ORDER_SHIPPED");
        when(orderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));
        doThrow(new RuntimeException("smtp down")).when(emailNotificationPort).sendOrderShipped(order);

        assertThatCode(() -> consumer.consume(event)).doesNotThrowAnyException();

        verify(processedEventRepositoryPort).save(event.id());
    }

    @Test
    void shouldNotSendEmailForDuplicateEvent() {
        OrderShippedConsumer consumer = new OrderShippedConsumer(
                processedEventRepositoryPort, orderRepositoryPort, emailNotificationPort);
        OutboxEvent event = eventFor("ORDER_SHIPPED");
        when(processedEventRepositoryPort.existsById(event.id())).thenReturn(true);

        consumer.consume(event);

        verify(emailNotificationPort, never()).sendOrderShipped(any());
    }
}