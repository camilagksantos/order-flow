package com.camilagksantos.orderflow.application.mapper;

import com.camilagksantos.orderflow.application.dto.request.CreateProductRequest;
import com.camilagksantos.orderflow.application.dto.request.UpdateProductRequest;
import com.camilagksantos.orderflow.application.dto.response.ProductResponse;
import com.camilagksantos.orderflow.domain.product.Product;
import com.camilagksantos.orderflow.domain.shared.Money;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

@Mapper(componentModel = "spring", uses = {CategoryMapper.class})
public interface ProductMapper {

    @Mapping(target = "availableQuantity", expression = "java(domain.availableQuantity())")
    ProductResponse toResponse(Product domain);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "price", source = "price")
    @Mapping(target = "reservedQuantity", constant = "0")
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "status", ignore = true)
    Product toDomain(CreateProductRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sku", ignore = true)
    @Mapping(target = "price", source = "price")
    @Mapping(target = "reservedQuantity", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "status", ignore = true)
    Product toDomain(UpdateProductRequest request);

    default Money toMoney(BigDecimal value) {
        return Money.of(value);
    }
}