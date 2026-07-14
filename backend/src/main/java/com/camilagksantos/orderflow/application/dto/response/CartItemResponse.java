package com.camilagksantos.orderflow.application.dto.response;

import com.camilagksantos.orderflow.domain.cart.CartStatus;
import com.camilagksantos.orderflow.domain.shared.Money;

import java.util.List;

public record CartItemResponse(
        String id,
        Long productId,
        String productName,
        String productSku,
        Money unitPrice,
        int quantity,
        Money subtotal
) {}