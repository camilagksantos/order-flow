package com.camilagksantos.orderflow.domain.exception;

public class OrderNotFoundException extends ResourceNotFoundException {

    public OrderNotFoundException(String id) {
        super("Order not found with id: " + id);
    }
}