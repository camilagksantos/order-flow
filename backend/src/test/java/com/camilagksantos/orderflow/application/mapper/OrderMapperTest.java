package com.camilagksantos.orderflow.application.mapper;

import com.camilagksantos.orderflow.application.dto.response.OrderResponse;
import com.camilagksantos.orderflow.domain.order.OrderStatus;
import com.camilagksantos.orderflow.domain.order.PaymentMethod;
import com.camilagksantos.orderflow.domain.order.ShopOrder;
import com.camilagksantos.orderflow.domain.shared.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class OrderMapperTest {

    private final OrderMapper orderMapper = new OrderMapperImpl();

    @Test
    void shouldMapShopOrderToResponse() {
        ShopOrder order = ShopOrder.builder()
                .id(UUID.randomUUID().toString())
                .orderNumber("ORD-TEST-001")
                .customerId(1L)
                .customerEmail("camila@test.com")
                .status(OrderStatus.PENDING)
                .items(List.of())
                .subtotal(Money.of(BigDecimal.valueOf(100)))
                .shippingCost(Money.zero())
                .discountAmount(Money.zero())
                .totalAmount(Money.of(BigDecimal.valueOf(100)))
                .paymentMethod(PaymentMethod.MBWAY)
                .idempotencyKey(UUID.randomUUID().toString())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        OrderResponse response = orderMapper.toResponse(order);

        assertThat(response.orderNumber()).isEqualTo("ORD-TEST-001");
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.totalAmount().amount()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }
}