package com.camilagksantos.orderflow.application.service;

import com.camilagksantos.orderflow.application.port.input.ProcessPaymentUseCase;
import com.camilagksantos.orderflow.application.port.output.OrderRepositoryPort;
import com.camilagksantos.orderflow.application.port.output.OutboxEventRepositoryPort;
import com.camilagksantos.orderflow.application.port.output.PaymentRepositoryPort;
import com.camilagksantos.orderflow.domain.event.OutboxEvent;
import com.camilagksantos.orderflow.domain.event.OutboxEventStatus;
import com.camilagksantos.orderflow.domain.exception.BusinessRuleException;
import com.camilagksantos.orderflow.domain.exception.OrderNotFoundException;
import com.camilagksantos.orderflow.domain.order.OrderStatus;
import com.camilagksantos.orderflow.domain.order.ShopOrder;
import com.camilagksantos.orderflow.domain.payment.Payment;
import com.camilagksantos.orderflow.domain.payment.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService implements ProcessPaymentUseCase {

    private final PaymentRepositoryPort paymentRepositoryPort;
    private final OrderRepositoryPort orderRepositoryPort;
    private final OutboxEventRepositoryPort outboxEventRepositoryPort;

    @Override
    @Transactional
    public Payment processPayment(Payment payment) {
        ShopOrder order = orderRepositoryPort.findById(payment.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException(payment.getOrderId()));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessRuleException("Order is not awaiting payment: " + order.getStatus());
        }

        if (order.getPaymentMethod() != payment.getMethod()) {
            throw new BusinessRuleException("Payment method does not match the order: " + order.getPaymentMethod());
        }

        paymentRepositoryPort.findByOrderId(order.getId())
                .ifPresent(existing -> {
                    throw new BusinessRuleException("Payment already exists for order: " + order.getId());
                });

        payment.setId(UUID.randomUUID().toString());
        payment.setAmount(order.getTotalAmount());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setAttemptCount(0);
        payment.setCreatedAt(LocalDateTime.now());
        payment.approve("SIM-" + UUID.randomUUID());

        Payment savedPayment = paymentRepositoryPort.save(payment);

        order.pay();
        orderRepositoryPort.save(order);

        outboxEventRepositoryPort.save(new OutboxEvent(
                UUID.randomUUID().toString(),
                "ORDER_PAID",
                order.getId(),
                OutboxEventStatus.PENDING,
                LocalDateTime.now()
        ));

        return savedPayment;
    }
}