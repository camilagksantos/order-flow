package com.camilagksantos.orderflow.application.port.input;

import com.camilagksantos.orderflow.domain.cart.Cart;

public interface RemoveFromCartUseCase {

    Cart  execute(Long customerId, String itemId);
}
