package com.camilagksantos.orderflow.application.dto.response;

import com.camilagksantos.orderflow.domain.order.OrderStatus;
import com.camilagksantos.orderflow.domain.order.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        String id,
        String orderNumber,
        Long customerId,
        OrderStatus status,
        List<OrderItemResponse> items,
        BigDecimal subtotal,
        BigDecimal shippingCost,
        BigDecimal discountAmount,
        BigDecimal totalAmount,
        PaymentMethod paymentMethod,
        String trackingCode,
        LocalDateTime createdAt,
        LocalDateTime paidAt,
        LocalDateTime shippedAt,
        LocalDateTime deliveredAt,
        LocalDateTime cancelledAt
) {}