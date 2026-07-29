package com.camilagksantos.orderflow.domain.product;

import com.camilagksantos.orderflow.domain.category.Category;
import com.camilagksantos.orderflow.domain.exception.InsufficientStockException;
import com.camilagksantos.orderflow.domain.shared.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class ProductTest {

    private Product product;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .name("Test Product")
                .sku("TEST-001")
                .price(Money.of(BigDecimal.valueOf(50)))
                .stockQuantity(10)
                .reservedQuantity(0)
                .category(Category.builder().id(1L).name("Test Category").build())
                .status(ProductStatus.ACTIVE)
                .build();
    }

    @Test
    void shouldCalculateAvailableQuantity() {
        assertThat(product.availableQuantity()).isEqualTo(10);
    }

    @Test
    void shouldReserveStock() {
        product.reserve(3);
        assertThat(product.getReservedQuantity()).isEqualTo(3);
        assertThat(product.availableQuantity()).isEqualTo(7);
    }

    @Test
    void shouldThrowWhenInsufficientStock() {
        assertThatThrownBy(() -> product.reserve(15))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void shouldReleaseReservedStock() {
        product.reserve(3);
        product.release(3);
        assertThat(product.getReservedQuantity()).isEqualTo(0);
        assertThat(product.availableQuantity()).isEqualTo(10);
    }

    @Test
    void shouldConfirmSale() {
        product.reserve(3);
        product.confirmSale(3);
        assertThat(product.getStockQuantity()).isEqualTo(7);
        assertThat(product.getReservedQuantity()).isEqualTo(0);
        assertThat(product.availableQuantity()).isEqualTo(7);
    }

    @Test
    void shouldDeactivateProduct() {
        product.deactivate();
        assertThat(product.getStatus()).isEqualTo(ProductStatus.INACTIVE);
    }

    @Test
    void shouldActivateProduct() {
        product.deactivate();
        product.activate();
        assertThat(product.getStatus()).isEqualTo(ProductStatus.ACTIVE);
    }

    @Test
    void shouldReturnTrueWhenHasAvailableStock() {
        assertThat(product.hasAvailableStock(5)).isTrue();
    }

    @Test
    void shouldReturnFalseWhenNotEnoughStock() {
        assertThat(product.hasAvailableStock(15)).isFalse();
    }
}