package com.camilagksantos.orderflow.domain.exception;

public class CartNotFoundException extends ResourceNotFoundException {

    public CartNotFoundException(String id) {
        super("Cart not found with id: " + id);
    }
}