package com.camilagksantos.orderflow.application.dto.response;

import com.camilagksantos.orderflow.domain.order.PaymentMethod;
import com.camilagksantos.orderflow.domain.payment.PaymentStatus;
import com.camilagksantos.orderflow.domain.shared.Money;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        String id,
        String orderId,
        Money amount,
        PaymentMethod method,
        PaymentStatus status,
        String transactionId,
        LocalDateTime processedAt,
        LocalDateTime createdAt
) {}