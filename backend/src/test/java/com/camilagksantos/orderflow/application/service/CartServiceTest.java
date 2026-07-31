package com.camilagksantos.orderflow.application.service;

import com.camilagksantos.orderflow.application.port.output.CartRepositoryPort;
import com.camilagksantos.orderflow.application.port.output.ProductRepositoryPort;
import com.camilagksantos.orderflow.domain.cart.Cart;
import com.camilagksantos.orderflow.domain.cart.CartItem;
import com.camilagksantos.orderflow.domain.cart.CartStatus;
import com.camilagksantos.orderflow.domain.exception.CartNotFoundException;
import com.camilagksantos.orderflow.domain.exception.ProductNotFoundException;
import com.camilagksantos.orderflow.domain.product.Product;
import com.camilagksantos.orderflow.domain.product.ProductStatus;
import com.camilagksantos.orderflow.domain.shared.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepositoryPort cartRepositoryPort;

    @Mock
    private ProductRepositoryPort productRepositoryPort;

    @InjectMocks
    private CartService cartService;

    private Cart cart;
    private CartItem item;
    private Product product;

    @BeforeEach
    void setUp() {
        cart = Cart.builder()
                .id(UUID.randomUUID().toString())
                .customerId(1L)
                .status(CartStatus.ACTIVE)
                .items(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        item = CartItem.builder()
                .id(UUID.randomUUID().toString())
                .productId(1L)
                .productName("Product A")
                .productSku("SKU-001")
                .unitPrice(Money.of(BigDecimal.valueOf(10)))
                .quantity(2)
                .build();

        product = Product.builder()
                .id(1L)
                .name("Product A")
                .sku("SKU-001")
                .price(Money.of(BigDecimal.valueOf(10)))
                .stockQuantity(50)
                .reservedQuantity(0)
                .status(ProductStatus.ACTIVE)
                .build();
    }

    @Test
    void shouldAddItemToExistingCart() {
        when(productRepositoryPort.findById(1L)).thenReturn(Optional.of(product));
        when(cartRepositoryPort.findActiveByCustomerId(1L)).thenReturn(Optional.of(cart));
        when(cartRepositoryPort.save(any())).thenReturn(cart);

        Cart result = cartService.addToCart(1L, 1L, 2);

        assertThat(result).isNotNull();
        verify(cartRepositoryPort).save(any());
    }

    @Test
    void shouldCreateNewCartWhenNoneExists() {
        when(productRepositoryPort.findById(1L)).thenReturn(Optional.of(product));
        when(cartRepositoryPort.findActiveByCustomerId(1L)).thenReturn(Optional.empty());
        when(cartRepositoryPort.save(any())).thenReturn(cart);

        cartService.addToCart(1L, 1L, 2);

        verify(cartRepositoryPort, times(2)).save(any());
    }

    @Test
    void shouldThrowWhenProductNotFound() {
        when(productRepositoryPort.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addToCart(1L, 99L, 1))
                .isInstanceOf(ProductNotFoundException.class);

        verify(cartRepositoryPort, never()).save(any());
    }

    @Test
    void shouldRemoveItemFromCart() {
        cart.addItem(item);
        when(cartRepositoryPort.findActiveByCustomerId(1L)).thenReturn(Optional.of(cart));
        when(cartRepositoryPort.save(any())).thenReturn(cart);

        Cart result = cartService.removeFromCart(1L, item.getId());

        assertThat(result).isNotNull();
        verify(cartRepositoryPort).save(any());
    }

    @Test
    void shouldFindCartByCustomerId() {
        when(cartRepositoryPort.findActiveByCustomerId(1L)).thenReturn(Optional.of(cart));

        Cart found = cartService.findCartByCustomerId(1L);

        assertThat(found).isNotNull();
        assertThat(found.getCustomerId()).isEqualTo(1L);
    }

    @Test
    void shouldThrowWhenCartNotFound() {
        when(cartRepositoryPort.findActiveByCustomerId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.findCartByCustomerId(99L))
                .isInstanceOf(CartNotFoundException.class);
    }
}