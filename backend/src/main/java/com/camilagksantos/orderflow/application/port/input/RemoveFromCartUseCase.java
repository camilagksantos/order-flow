package com.camilagksantos.orderflow.application.port.input;

import com.camilagksantos.orderflow.domain.cart.Cart;

public interface RemoveFromCartUseCase {

    Cart  removeFromCart(Long customerId, String itemId);
}
