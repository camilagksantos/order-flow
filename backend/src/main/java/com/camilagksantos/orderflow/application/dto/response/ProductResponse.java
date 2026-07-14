package com.camilagksantos.orderflow.application.dto.response;

import com.camilagksantos.orderflow.domain.product.ProductStatus;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String name,
        String description,
        String sku,
        BigDecimal price,
        int stockQuantity,
        int reservedQuantity,
        int availableQuantity,
        CategoryResponse category,
        String imageUrl,
        ProductStatus status
) {}