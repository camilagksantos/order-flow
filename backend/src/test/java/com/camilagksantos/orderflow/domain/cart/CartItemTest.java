package com.camilagksantos.orderflow.domain.cart;

import com.camilagksantos.orderflow.domain.shared.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class CartItemTest {

    @Test
    void shouldCalculateSubtotal() {
        CartItem item = CartItem.builder()
                .id(UUID.randomUUID().toString())
                .productId(1L)
                .productName("Product A")
                .productSku("SKU-001")
                .unitPrice(Money.of(BigDecimal.valueOf(15)))
                .quantity(3)
                .build();

        assertThat(item.subtotal().amount()).isEqualByComparingTo(BigDecimal.valueOf(45));
    }

    @Test
    void shouldCalculateSubtotalWithQuantityOne() {
        CartItem item = CartItem.builder()
                .id(UUID.randomUUID().toString())
                .productId(1L)
                .productName("Product A")
                .productSku("SKU-001")
                .unitPrice(Money.of(BigDecimal.valueOf(25)))
                .quantity(1)
                .build();

        assertThat(item.subtotal().amount()).isEqualByComparingTo(BigDecimal.valueOf(25));
    }
}