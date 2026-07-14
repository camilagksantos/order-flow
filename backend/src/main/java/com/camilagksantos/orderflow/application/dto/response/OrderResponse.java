package com.camilagksantos.orderflow.application.dto.response;

import com.camilagksantos.orderflow.domain.order.OrderStatus;
import com.camilagksantos.orderflow.domain.order.PaymentMethod;
import com.camilagksantos.orderflow.domain.shared.Money;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        String id,
        String orderNumber,
        Long customerId,
        OrderStatus status,
        List<OrderItemResponse> items,
        Money subtotal,
        Money shippingCost,
        Money discountAmount,
        Money totalAmount,
        PaymentMethod paymentMethod,
        String trackingCode,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        LocalDateTime shippedAt,
        LocalDateTime deliveredAt,
        LocalDateTime cancelledAt
) {}