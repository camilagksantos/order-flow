package com.camilagksantos.orderflow.domain.order;

import com.camilagksantos.orderflow.domain.exception.InvalidOrderStatusTransitionException;
import com.camilagksantos.orderflow.domain.shared.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class ShopOrderTest {

    private ShopOrder order;

    @BeforeEach
    void setUp() {
        order = ShopOrder.builder()
                .id(UUID.randomUUID().toString())
                .orderNumber("ORD-TEST-001")
                .customerId(1L)
                .customerEmail("test@test.com")
                .status(OrderStatus.PENDING)
                .items(List.of())
                .subtotal(Money.of(BigDecimal.valueOf(100)))
                .shippingCost(Money.of(BigDecimal.valueOf(5)))
                .discountAmount(Money.zero())
                .totalAmount(Money.of(BigDecimal.valueOf(105)))
                .paymentMethod(PaymentMethod.MBWAY)
                .idempotencyKey(UUID.randomUUID().toString())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void shouldTransitionFromPendingToPaid() {
        order.pay();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getPaidAt()).isNotNull();
    }

    @Test
    void shouldTransitionFromPaidToPrepairing() {
        order.pay();
        order.startPreparing();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PREPARING);
    }

    @Test
    void shouldTransitionFromPreparingToShipped() {
        order.pay();
        order.startPreparing();
        order.ship("TRACK-123");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(order.getTrackingCode()).isEqualTo("TRACK-123");
        assertThat(order.getShippedAt()).isNotNull();
    }

    @Test
    void shouldTransitionFromShippedToDelivered() {
        order.pay();
        order.startPreparing();
        order.ship("TRACK-123");
        order.deliver();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(order.getDeliveredAt()).isNotNull();
    }

    @Test
    void shouldTransitionFromPendingToCancelled() {
        order.cancel("Customer request");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancelReason()).isEqualTo("Customer request");
        assertThat(order.getCancelledAt()).isNotNull();
    }

    @Test
    void shouldTransitionFromPreparingToCancelled() {
        order.pay();
        order.startPreparing();
        order.cancel("Out of stock");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void shouldThrowWhenInvalidTransitionFromPaidToShipped() {
        order.pay();
        assertThatThrownBy(() -> order.ship("TRACK-123"))
                .isInstanceOf(InvalidOrderStatusTransitionException.class);
    }

    @Test
    void shouldThrowWhenInvalidTransitionFromDeliveredToCancelled() {
        order.pay();
        order.startPreparing();
        order.ship("TRACK-123");
        order.deliver();
        assertThatThrownBy(() -> order.cancel("Too late"))
                .isInstanceOf(InvalidOrderStatusTransitionException.class);
    }

    @Test
    void shouldThrowWhenInvalidTransitionFromPendingToDelivered() {
        assertThatThrownBy(() -> order.deliver())
                .isInstanceOf(InvalidOrderStatusTransitionException.class);
    }
}