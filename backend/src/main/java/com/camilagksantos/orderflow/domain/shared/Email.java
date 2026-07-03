package com.camilagksantos.orderflow.domain.shared;

public record Email(
        String value
) {
    public Email {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Email must not be blank");
        if (!value.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) throw new IllegalArgumentException("Invalid email format");
        value = value.toLowerCase();
    }
}

