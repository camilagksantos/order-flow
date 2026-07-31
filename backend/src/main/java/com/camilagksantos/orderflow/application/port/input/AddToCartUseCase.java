package com.camilagksantos.orderflow.application.port.input;

import com.camilagksantos.orderflow.domain.cart.Cart;

public interface AddToCartUseCase {

    Cart addToCart(Long customerId, Long productId, int quantity);
}