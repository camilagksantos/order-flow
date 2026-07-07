package com.camilagksantos.orderflow.domain.payment;

import com.camilagksantos.orderflow.domain.order.PaymentMethod;
import com.camilagksantos.orderflow.domain.shared.Money;

import java.time.LocalDateTime;

public record Payment(
        String id,
        String orderId,
        Money amount,
        PaymentMethod method,
        PaymentStatus status,
        String transactionId,
        String cardLastFour,
        String cardBrand,
        String mbwayPhone,
        String mbEntity,
        String mbReference,
        int attemptCount,
        LocalDateTime processedAt,
        LocalDateTime createdAt
) {
    public Payment {
        if (orderId == null || orderId.isBlank()) throw new IllegalArgumentException("Order ID must not be null");
        if (amount == null) throw new IllegalArgumentException("Amount must not be null");
        if (method == null) throw new IllegalArgumentException("Payment method must not be null");
        if (status == null) throw new IllegalArgumentException("Status must not be null");
    }

    public Payment approve(String transactionId) {
        return new Payment(id, orderId, amount, method, PaymentStatus.APPROVED, transactionId, cardLastFour, cardBrand, mbwayPhone, mbEntity, mbReference, attemptCount, LocalDateTime.now(), createdAt);
    }

    public Payment decline() {
        return new Payment(id, orderId, amount, method, PaymentStatus.DECLINED, transactionId, cardLastFour, cardBrand, mbwayPhone, mbEntity, mbReference, attemptCount + 1, processedAt, createdAt);
    }
}
