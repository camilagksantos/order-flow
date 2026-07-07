package com.camilagksantos.orderflow.domain.auth;

import java.util.List;

public record User(
        Long id,
        String email,
        String password,
        boolean active,
        List<Role> roles
) {
    public User {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email must not be blank");
        if (password == null || password.isBlank()) throw new IllegalArgumentException("Password must not be blank");
        roles = roles == null ? List.of() : List.copyOf(roles);
    }

    public boolean hasRole(String roleName) {
        return roles.stream().anyMatch(role -> role.name().equals(roleName));
    }

    public User deactivate() {
        return new User(id, email, password, false, roles);
    }

    public User activate() {
        return new User(id, email, password, true, roles);
    }
}
