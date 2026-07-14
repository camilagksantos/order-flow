package com.camilagksantos.orderflow.application.dto.request;

import com.camilagksantos.orderflow.domain.order.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
        @NotNull OrderStatus status
) {}