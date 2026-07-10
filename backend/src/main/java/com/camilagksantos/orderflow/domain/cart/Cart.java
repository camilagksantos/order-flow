package com.camilagksantos.orderflow.domain.cart;

import com.camilagksantos.orderflow.domain.shared.Money;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cart {
    private String id;
    private Long customerId;
    private CartStatus status;
    private List<CartItem> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Money total() {
        return items.stream()
                .map(CartItem::subtotal)
                .reduce(Money.zero(), Money::add);
    }

    public boolean isEmpty() {
        return items == null || items.isEmpty();
    }

    public void addItem(CartItem item) {
        if (this.items == null) this.items = new ArrayList<>();
        this.items.add(item);
        this.updatedAt = LocalDateTime.now();
    }

    public void removeItem(String itemId) {
        if (this.items != null) {
            this.items.removeIf(item -> item.getId().equals(itemId));
            this.updatedAt = LocalDateTime.now();
        }
    }

    public void convert() {
        this.status = CartStatus.CONVERTED;
        this.updatedAt = LocalDateTime.now();
    }

    public static Cart newCart(Long customerId) {
        return Cart.builder()
                .id(UUID.randomUUID().toString())
                .customerId(customerId)
                .status(CartStatus.ACTIVE)
                .items(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}