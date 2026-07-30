package com.camilagksantos.orderflow.infrastructure.adapter.output.persistence;

import com.camilagksantos.orderflow.BaseIntegrationTest;
import com.camilagksantos.orderflow.domain.category.Category;
import com.camilagksantos.orderflow.domain.product.Product;
import com.camilagksantos.orderflow.domain.product.ProductStatus;
import com.camilagksantos.orderflow.domain.shared.Money;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@Transactional
class ProductFlowIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private CategoryJpaAdapter categoryJpaAdapter;

    @Autowired
    private ProductJpaAdapter productJpaAdapter;

    @Test
    void shouldCreateCategoryAndProduct() {
        Category category = categoryJpaAdapter.save(
                Category.builder().name("Electronics").build()
        );

        Product product = Product.builder()
                .name("Laptop")
                .sku("LAP-001")
                .price(Money.of(BigDecimal.valueOf(999)))
                .stockQuantity(10)
                .reservedQuantity(0)
                .category(category)
                .status(ProductStatus.ACTIVE)
                .build();

        Product saved = productJpaAdapter.save(product);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Laptop");
        assertThat(saved.getPrice().amount()).isEqualByComparingTo(BigDecimal.valueOf(999));
        assertThat(saved.getCategory().getName()).isEqualTo("Electronics");
    }

    @Test
    void shouldFindProductById() {
        Category category = categoryJpaAdapter.save(
                Category.builder().name("Books").build()
        );

        Product product = productJpaAdapter.save(
                Product.builder()
                        .name("Clean Code")
                        .sku("BOOK-001")
                        .price(Money.of(BigDecimal.valueOf(30)))
                        .stockQuantity(5)
                        .reservedQuantity(0)
                        .category(category)
                        .status(ProductStatus.ACTIVE)
                        .build()
        );

        Optional<Product> found = productJpaAdapter.findById(product.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Clean Code");
    }

    @Test
    void shouldFindProductBySku() {
        Category category = categoryJpaAdapter.save(
                Category.builder().name("Music").build()
        );

        productJpaAdapter.save(
                Product.builder()
                        .name("Guitar")
                        .sku("GTR-001")
                        .price(Money.of(BigDecimal.valueOf(200)))
                        .stockQuantity(3)
                        .reservedQuantity(0)
                        .category(category)
                        .status(ProductStatus.ACTIVE)
                        .build()
        );

        Optional<Product> found = productJpaAdapter.findBySku("GTR-001");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Guitar");
    }

    @Test
    void shouldFindAllProducts() {
        Category category = categoryJpaAdapter.save(
                Category.builder().name("Sports").build()
        );

        productJpaAdapter.save(Product.builder()
                .name("Football")
                .sku("BALL-001")
                .price(Money.of(BigDecimal.valueOf(20)))
                .stockQuantity(50)
                .reservedQuantity(0)
                .category(category)
                .status(ProductStatus.ACTIVE)
                .build());

        List<Product> products = productJpaAdapter.findAll();

        assertThat(products).isNotEmpty();
    }

    @Test
    void shouldReserveAndReleaseStock() {
        Category category = categoryJpaAdapter.save(
                Category.builder().name("Clothing").build()
        );

        Product product = productJpaAdapter.save(
                Product.builder()
                        .name("T-Shirt")
                        .sku("TSH-001")
                        .price(Money.of(BigDecimal.valueOf(15)))
                        .stockQuantity(20)
                        .reservedQuantity(0)
                        .category(category)
                        .status(ProductStatus.ACTIVE)
                        .build()
        );

        product.reserve(5);
        productJpaAdapter.save(product);

        Product reserved = productJpaAdapter.findById(product.getId()).orElseThrow();
        assertThat(reserved.getReservedQuantity()).isEqualTo(5);
        assertThat(reserved.availableQuantity()).isEqualTo(15);

        reserved.release(5);
        productJpaAdapter.save(reserved);

        Product released = productJpaAdapter.findById(product.getId()).orElseThrow();
        assertThat(released.getReservedQuantity()).isEqualTo(0);
        assertThat(released.availableQuantity()).isEqualTo(20);
    }
}