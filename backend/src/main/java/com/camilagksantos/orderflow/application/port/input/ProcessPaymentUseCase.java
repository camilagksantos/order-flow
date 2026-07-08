package com.camilagksantos.orderflow.application.port.input;

import com.camilagksantos.orderflow.domain.payment.Payment;

public interface ProcessPaymentUseCase {
    Payment execute(Payment payment);
}