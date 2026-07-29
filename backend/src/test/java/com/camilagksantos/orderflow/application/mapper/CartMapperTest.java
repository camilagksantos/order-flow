package com.camilagksantos.orderflow.application.mapper;

import com.camilagksantos.orderflow.application.dto.response.CartItemResponse;
import com.camilagksantos.orderflow.application.dto.response.CartResponse;
import com.camilagksantos.orderflow.domain.cart.Cart;
import com.camilagksantos.orderflow.domain.cart.CartItem;
import com.camilagksantos.orderflow.domain.cart.CartStatus;
import com.camilagksantos.orderflow.domain.shared.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class CartMapperTest {

    private final CartMapper cartMapper = new CartMapperImpl();

    @Test
    void shouldMapCartToResponse() {
        CartItem item = CartItem.builder()
                .id(UUID.randomUUID().toString())
                .productId(1L)
                .productName("Product A")
                .productSku("SKU-001")
                .unitPrice(Money.of(BigDecimal.valueOf(10)))
                .quantity(2)
                .build();

        Cart cart = Cart.builder()
                .id(UUID.randomUUID().toString())
                .customerId(1L)
                .status(CartStatus.ACTIVE)
                .items(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        cart.addItem(item);

        CartResponse response = cartMapper.toResponse(cart);

        assertThat(response.customerId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(CartStatus.ACTIVE);
        assertThat(response.total().amount()).isEqualByComparingTo(BigDecimal.valueOf(20));
    }

    @Test
    void shouldMapCartItemToResponse() {
        CartItem item = CartItem.builder()
                .id(UUID.randomUUID().toString())
                .productId(1L)
                .productName("Product A")
                .productSku("SKU-001")
                .unitPrice(Money.of(BigDecimal.valueOf(15)))
                .quantity(3)
                .build();

        CartItemResponse response = cartMapper.toItemResponse(item);

        assertThat(response.productName()).isEqualTo("Product A");
        assertThat(response.unitPrice().amount()).isEqualByComparingTo(BigDecimal.valueOf(15));
        assertThat(response.subtotal().amount()).isEqualByComparingTo(BigDecimal.valueOf(45));
    }
}