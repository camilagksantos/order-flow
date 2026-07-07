package com.camilagksantos.orderflow.domain.exception;

public class InsufficientStockException extends BusinessRuleException {

    public InsufficientStockException(Long productId, int requested, int available) {
        super("Insufficient stock for product " + productId + ". Requested: " + requested + ", Available: " + available);
    }
}