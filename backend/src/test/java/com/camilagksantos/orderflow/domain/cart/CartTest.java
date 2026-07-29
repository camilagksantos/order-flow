package com.camilagksantos.orderflow.domain.cart;

import com.camilagksantos.orderflow.domain.shared.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class CartTest {

    private Cart cart;
    private CartItem item1;
    private CartItem item2;

    @BeforeEach
    void setUp() {
        cart = Cart.newCart(1L);

        item1 = CartItem.builder()
                .id(UUID.randomUUID().toString())
                .cartId(cart.getId())
                .productId(1L)
                .productName("Product A")
                .productSku("SKU-001")
                .unitPrice(Money.of(BigDecimal.valueOf(10)))
                .quantity(2)
                .build();

        item2 = CartItem.builder()
                .id(UUID.randomUUID().toString())
                .cartId(cart.getId())
                .productId(2L)
                .productName("Product B")
                .productSku("SKU-002")
                .unitPrice(Money.of(BigDecimal.valueOf(20)))
                .quantity(1)
                .build();
    }

    @Test
    void shouldCreateNewCartWithActiveStatus() {
        assertThat(cart.getStatus()).isEqualTo(CartStatus.ACTIVE);
        assertThat(cart.getCustomerId()).isEqualTo(1L);
        assertThat(cart.isEmpty()).isTrue();
    }

    @Test
    void shouldAddItemToCart() {
        cart.addItem(item1);
        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.isEmpty()).isFalse();
    }

    @Test
    void shouldAddMultipleItemsToCart() {
        cart.addItem(item1);
        cart.addItem(item2);
        assertThat(cart.getItems()).hasSize(2);
    }

    @Test
    void shouldRemoveItemFromCart() {
        cart.addItem(item1);
        cart.addItem(item2);
        cart.removeItem(item1.getId());
        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getId()).isEqualTo(item2.getId());
    }

    @Test
    void shouldCalculateTotalCorrectly() {
        cart.addItem(item1);
        cart.addItem(item2);
        Money total = cart.total();
        assertThat(total.amount()).isEqualByComparingTo(BigDecimal.valueOf(40));
    }

    @Test
    void shouldReturnZeroTotalForEmptyCart() {
        Money total = cart.total();
        assertThat(total.amount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldConvertCartToConverted() {
        cart.addItem(item1);
        cart.convert();
        assertThat(cart.getStatus()).isEqualTo(CartStatus.CONVERTED);
    }

    @Test
    void shouldCalculateCartItemSubtotal() {
        Money subtotal = item1.subtotal();
        assertThat(subtotal.amount()).isEqualByComparingTo(BigDecimal.valueOf(20));
    }
}