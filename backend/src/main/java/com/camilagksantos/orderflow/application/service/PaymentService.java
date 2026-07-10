package com.camilagksantos.orderflow.application.service;

import com.camilagksantos.orderflow.application.port.input.ProcessPaymentUseCase;
import com.camilagksantos.orderflow.application.port.output.PaymentRepositoryPort;
import com.camilagksantos.orderflow.domain.payment.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService implements ProcessPaymentUseCase {

    private final PaymentRepositoryPort paymentRepositoryPort;

    @Override
    public Payment processPayment(Payment payment) {
        return paymentRepositoryPort.save(payment);
    }
}