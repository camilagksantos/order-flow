package com.camilagksantos.orderflow.domain.category;

public record Category(
        Long id, String name
) {
    public Category {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Category name must not be blank");
    }
}
