package com.camilagksantos.orderflow.domain.exception;

public class AddressNotFoundException extends ResourceNotFoundException {

    public AddressNotFoundException(Long id) {
        super("Address not found with id: " + id);
    }
}