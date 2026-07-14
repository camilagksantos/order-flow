package com.camilagksantos.orderflow.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(
        @NotBlank String idempotencyKey,
        @NotNull Long addressId
) {}