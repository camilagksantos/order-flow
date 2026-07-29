package com.camilagksantos.orderflow.application.mapper;

import com.camilagksantos.orderflow.application.dto.request.CreateProductRequest;
import com.camilagksantos.orderflow.application.dto.response.ProductResponse;
import com.camilagksantos.orderflow.domain.category.Category;
import com.camilagksantos.orderflow.domain.product.Product;
import com.camilagksantos.orderflow.domain.product.ProductStatus;
import com.camilagksantos.orderflow.domain.shared.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class ProductMapperTest {

    private final CategoryMapper categoryMapper = new CategoryMapperImpl();
    private final ProductMapper productMapper;

    ProductMapperTest() throws Exception {
        ProductMapperImpl impl = new ProductMapperImpl();
        var field = ProductMapperImpl.class.getDeclaredField("categoryMapper");
        field.setAccessible(true);
        field.set(impl, categoryMapper);
        this.productMapper = impl;
    }

    @Test
    void shouldMapProductToResponse() {
        Product product = Product.builder()
                .id(1L)
                .name("Laptop")
                .sku("LAP-001")
                .price(Money.of(BigDecimal.valueOf(999)))
                .stockQuantity(10)
                .reservedQuantity(2)
                .category(Category.builder().id(1L).name("Electronics").build())
                .status(ProductStatus.ACTIVE)
                .build();

        ProductResponse response = productMapper.toResponse(product);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Laptop");
        assertThat(response.price().amount()).isEqualByComparingTo(BigDecimal.valueOf(999));
        assertThat(response.availableQuantity()).isEqualTo(8);
    }

    @Test
    void shouldMapCreateRequestToDomain() {
        CreateProductRequest request = new CreateProductRequest(
                "Laptop", "A great laptop", "LAP-001",
                BigDecimal.valueOf(999), 10, 1L, null
        );

        Product product = productMapper.toDomain(request);

        assertThat(product.getName()).isEqualTo("Laptop");
        assertThat(product.getSku()).isEqualTo("LAP-001");
        assertThat(product.getPrice().amount()).isEqualByComparingTo(BigDecimal.valueOf(999));
        assertThat(product.getReservedQuantity()).isEqualTo(0);
    }
}