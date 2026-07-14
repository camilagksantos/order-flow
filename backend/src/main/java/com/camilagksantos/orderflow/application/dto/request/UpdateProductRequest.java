package com.camilagksantos.orderflow.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateProductRequest(
        @NotBlank @Size(min = 2, max = 150) String name,
        @Size(max = 2000) String description,
        @NotNull @DecimalMin("0.01") BigDecimal price,
        @Min(0) int stockQuantity,
        @NotNull Long categoryId,
        String imageUrl
) {}