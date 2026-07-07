package com.camilagksantos.orderflow.domain.product;

import com.camilagksantos.orderflow.domain.category.Category;
import com.camilagksantos.orderflow.domain.shared.Money;

public record Product(
        Long id,
        String name,
        String description,
        String sku,
        Money price,
        int stockQuantity,
        int reservedQuantity,
        Category category,
        String imageUrl,
        ProductStatus status
) {

    public Product {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Product name must not be blank");
        if (sku == null || sku.isBlank()) throw new IllegalArgumentException("SKU must not be blank");
        if (price == null) throw new IllegalArgumentException("Price must not be null");
        if (category == null) throw new IllegalArgumentException("Category must not be null");
        if (stockQuantity < 0) throw new IllegalArgumentException("Stock quantity must not be negative");
        if (reservedQuantity < 0) throw new IllegalArgumentException("Reserved quantity must not be negative");
    }

    public int availableQuantity() {
        return stockQuantity - reservedQuantity;
    }

    public boolean hasAvailableStock(int quantity) {
        return availableQuantity() >= quantity;
    }

    public Product reserve(int quantity) {
        if (!hasAvailableStock(quantity)) throw new IllegalArgumentException("Insufficient stock");
        return new Product(id, name, description, sku, price, stockQuantity, reservedQuantity + quantity, category, imageUrl, status);
    }

    public Product release(int quantity) {
        return new Product(id, name, description, sku, price, stockQuantity, reservedQuantity - quantity, category, imageUrl, status);
    }

    public Product deactivate() {
        return new Product(id, name, description, sku, price, stockQuantity, reservedQuantity, category, imageUrl, ProductStatus.INACTIVE);
    }

    public Product activate() {
        return new Product(id, name, description, sku, price, stockQuantity, reservedQuantity, category, imageUrl, ProductStatus.ACTIVE);
    }
}