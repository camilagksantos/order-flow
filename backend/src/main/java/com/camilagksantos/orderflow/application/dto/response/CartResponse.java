package com.camilagksantos.orderflow.application.dto.response;

import com.camilagksantos.orderflow.domain.cart.CartStatus;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        String id,
        Long customerId,
        CartStatus status,
        List<CartItemResponse> items,
        BigDecimal total
) {}