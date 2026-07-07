package com.camilagksantos.orderflow.domain.auth;

public record Role(
        Long id,
        String name
) {
    public Role {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Role name must not be blank");
    }
}
