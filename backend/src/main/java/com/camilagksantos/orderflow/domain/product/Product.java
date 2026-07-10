package com.camilagksantos.orderflow.domain.product;

import com.camilagksantos.orderflow.domain.category.Category;
import com.camilagksantos.orderflow.domain.shared.Money;
import com.camilagksantos.orderflow.domain.exception.InsufficientStockException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    private Long id;
    private String name;
    private String description;
    private String sku;
    private Money price;
    private int stockQuantity;
    private int reservedQuantity;
    private Category category;
    private String imageUrl;
    private ProductStatus status;

    public int availableQuantity() {
        return stockQuantity - reservedQuantity;
    }

    public boolean hasAvailableStock(int quantity) {
        return availableQuantity() >= quantity;
    }

    public void reserve(int quantity) {
        if (!hasAvailableStock(quantity)) {
            throw new InsufficientStockException(id, quantity, availableQuantity());
        }
        this.reservedQuantity += quantity;
    }

    public void release(int quantity) {
        this.reservedQuantity -= quantity;
    }

    public void deactivate() {
        this.status = ProductStatus.INACTIVE;
    }

    public void activate() {
        this.status = ProductStatus.ACTIVE;
    }
}