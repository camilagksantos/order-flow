package com.camilagksantos.orderflow.application.dto.request;

import com.camilagksantos.orderflow.domain.order.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProcessPaymentRequest(
        @NotBlank String orderId,
        @NotNull PaymentMethod method,
        String cardLastFour,
        String cardBrand,
        String mbwayPhone,
        String mbEntity,
        String mbReference
) {}