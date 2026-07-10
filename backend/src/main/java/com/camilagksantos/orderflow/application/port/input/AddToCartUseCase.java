package com.camilagksantos.orderflow.application.port.input;

import com.camilagksantos.orderflow.domain.cart.Cart;
import com.camilagksantos.orderflow.domain.cart.CartItem;

public interface AddToCartUseCase {

    Cart addToCart(Long customerId, CartItem item);
}
