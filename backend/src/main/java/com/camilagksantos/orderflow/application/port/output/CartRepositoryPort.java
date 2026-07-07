package com.camilagksantos.orderflow.application.port.output;

import com.camilagksantos.orderflow.domain.cart.Cart;

import java.util.Optional;

public interface CartRepositoryPort {

    Cart save(Cart cart);
    Optional<Cart> findById(String id);
    Optional<Cart> findActiveByCustomerId(Long customerId);
}
