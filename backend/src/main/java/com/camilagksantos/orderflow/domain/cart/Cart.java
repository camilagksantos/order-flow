package com.camilagksantos.orderflow.domain.cart;

import com.camilagksantos.orderflow.domain.shared.Money;

import java.time.LocalDateTime;
import java.util.List;

public record Cart(
        String id,
        Long customerId,
        CartStatus status,
        List<CartItem> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public Cart {
        if (customerId == null) throw new IllegalArgumentException("Customer ID must not be null");
        if (status == null) throw new IllegalArgumentException("Status must not be null");
        items = items == null ? List.of() : List.copyOf(items);
    }

    public Money total() {
        return items.stream()
                .map(CartItem::subtotal)
                .reduce(Money.zero(), Money::add);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public Cart addItem(CartItem item) {
        List<CartItem> updated = new java.util.ArrayList<>(items);
        updated.add(item);
        return new Cart(id, customerId, status, updated, createdAt, LocalDateTime.now());
    }

    public Cart removeItem(String itemId) {
        List<CartItem> updated = items.stream()
                .filter(item -> !item.id().equals(itemId))
                .toList();
        return new Cart(id, customerId, status, updated, createdAt, LocalDateTime.now());
    }

    public Cart convert() {
        return new Cart(id, customerId, CartStatus.CONVERTED, items, createdAt, LocalDateTime.now());
    }
}
