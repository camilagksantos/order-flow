package com.camilagksantos.orderflow.application.port.output;

import com.camilagksantos.orderflow.domain.payment.Payment;

import java.util.Optional;

public interface PaymentRepositoryPort {

    Payment save(Payment payment);
    Optional<Payment> findByOrderId(String orderId);
}
