package com.camilagksantos.orderflow.application.port.input;

import com.camilagksantos.orderflow.domain.cart.Cart;

public interface FindCartUseCase {

    Cart findCartByCustomerId(Long customerId);
}
