package com.camilagksantos.orderflow.application.dto.response;

import java.math.BigDecimal;

public record CartItemResponse(
        String id,
        Long productId,
        String productName,
        String productSku,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal subtotal
) {}