package com.camilagksantos.orderflow.domain.exception;

public class CustomerNotFoundException extends ResourceNotFoundException {

    public CustomerNotFoundException(Long id) {
        super("Customer not found with id: " + id);
    }
}