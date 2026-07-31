package com.camilagksantos.orderflow.application.dto.request;

import com.camilagksantos.orderflow.domain.order.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(
        @NotBlank String idempotencyKey,
        @NotNull Long addressId,
        @NotNull PaymentMethod paymentMethod
) {}