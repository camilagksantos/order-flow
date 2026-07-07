package com.camilagksantos.orderflow.domain.exception;

public class InvalidOrderStatusTransitionException extends BusinessRuleException {

    public InvalidOrderStatusTransitionException(String from, String to) {
        super("Invalid order status transition from " + from + " to " + to);
    }
}