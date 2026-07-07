package com.camilagksantos.orderflow.domain.cart;

import com.camilagksantos.orderflow.domain.shared.Money;

public record CartItem(
        String id,
        String cartId,
        Long productId,
        String productName,
        String productSku,
        Money unitPrice,
        int quantity
) {
    public CartItem {
        if (productId == null) throw new IllegalArgumentException("Product ID must not be null");
        if (productName == null || productName.isBlank()) throw new IllegalArgumentException("Product name must not be blank");
        if (productSku == null || productSku.isBlank()) throw new IllegalArgumentException("Product SKU must not be blank");
        if (unitPrice == null) throw new IllegalArgumentException("Unit price must not be null");
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be greater than zero");
    }

    public Money subtotal() {
        return unitPrice.multiply(quantity);
    }
}
