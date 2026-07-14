package com.camilagksantos.orderflow.application.dto.response;

import com.camilagksantos.orderflow.domain.product.ProductStatus;
import com.camilagksantos.orderflow.domain.shared.Money;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String name,
        String description,
        String sku,
        Money price,
        int stockQuantity,
        int reservedQuantity,
        int availableQuantity,
        CategoryResponse category,
        String imageUrl,
        ProductStatus status
) {}