package com.camilagksantos.orderflow.domain.customer;

import com.camilagksantos.orderflow.domain.shared.Email;
import com.camilagksantos.orderflow.domain.shared.NIF;

import java.util.List;

public record Customer(
        Long id,
        Long userId,
        String name,
        Email email,
        NIF nif,
        String phone,
        CustomerStatus status,
        List<Address> addresses
) {
    public Customer {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Name must not be blank");
        if (email == null) throw new IllegalArgumentException("Email must not be null");
        if (nif == null) throw new IllegalArgumentException("NIF must not be null");
        if (status == null) throw new IllegalArgumentException("Status must not be null");
        addresses = addresses == null ? List.of() : List.copyOf(addresses);
    }

    public Customer block() {
        return new Customer(id, userId, name, email, nif, phone, CustomerStatus.BLOCKED, addresses);
    }

    public Customer activate() {
        return new Customer(id, userId, name, email, nif, phone, CustomerStatus.ACTIVE, addresses);
    }
}
