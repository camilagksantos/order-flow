package com.camilagksantos.orderflow.application.service;

import com.camilagksantos.orderflow.application.port.input.AddToCartUseCase;
import com.camilagksantos.orderflow.application.port.input.FindCartUseCase;
import com.camilagksantos.orderflow.application.port.input.RemoveFromCartUseCase;
import com.camilagksantos.orderflow.application.port.output.CartRepositoryPort;
import com.camilagksantos.orderflow.domain.cart.Cart;
import com.camilagksantos.orderflow.domain.cart.CartItem;
import com.camilagksantos.orderflow.domain.exception.CartNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CartService implements AddToCartUseCase, RemoveFromCartUseCase, FindCartUseCase {

    private final CartRepositoryPort cartRepositoryPort;

    @Override
    public Cart addToCart(Long customerId, CartItem item) {
        Cart cart = cartRepositoryPort.findActiveByCustomerId(customerId)
                .orElseGet(() -> cartRepositoryPort.save(Cart.newCart(customerId)));
        cart.addItem(item);
        return cartRepositoryPort.save(cart);
    }

    @Override
    public Cart removeFromCart(Long customerId, String itemId) {
        Cart cart = findCartByCustomerId(customerId);
        cart.removeItem(itemId);
        return cartRepositoryPort.save(cart);
    }

    @Override
    public Cart findCartByCustomerId(Long customerId) {
        return cartRepositoryPort.findActiveByCustomerId(customerId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found for customer: " + customerId));
    }
}