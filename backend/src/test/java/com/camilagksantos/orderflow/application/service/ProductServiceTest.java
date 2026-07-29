package com.camilagksantos.orderflow.application.service;

import com.camilagksantos.orderflow.application.port.output.ProductRepositoryPort;
import com.camilagksantos.orderflow.domain.category.Category;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepositoryPort productRepositoryPort;

    @InjectMocks
    private ProductService productService;

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
    void shouldCreateProduct() {
        when(productRepositoryPort.save(product)).thenReturn(product);
        Product created = productService.createProduct(product);
        assertThat(created).isEqualTo(product);
        verify(productRepositoryPort).save(product);
    }

    @Test
    void shouldFindProductById() {
        when(productRepositoryPort.findById(1L)).thenReturn(Optional.of(product));
        Product found = productService.findProductById(1L);
        assertThat(found).isEqualTo(product);
    }

    @Test
    void shouldThrowWhenProductNotFound() {
        when(productRepositoryPort.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> productService.findProductById(99L))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void shouldFindAllProducts() {
        when(productRepositoryPort.findAll()).thenReturn(List.of(product));
        List<Product> products = productService.findAllProducts();
        assertThat(products).hasSize(1);
    }

    @Test
    void shouldDeleteProduct() {
        when(productRepositoryPort.findById(1L)).thenReturn(Optional.of(product));
        productService.deleteProduct(1L);
        verify(productRepositoryPort).deleteById(1L);
    }

    @Test
    void shouldThrowWhenDeletingNonExistentProduct() {
        when(productRepositoryPort.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> productService.deleteProduct(99L))
                .isInstanceOf(ProductNotFoundException.class);
    }
}