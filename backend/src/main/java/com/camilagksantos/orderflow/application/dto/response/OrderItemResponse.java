package com.camilagksantos.orderflow.application.dto.response;

import com.camilagksantos.orderflow.domain.shared.Money;

import java.math.BigDecimal;

public record OrderItemResponse(
        String id,
        Long productId,
        String productName,
        String productSku,
        Money unitPrice,
        int quantity,
        Money subtotal
) {}