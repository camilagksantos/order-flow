package com.camilagksantos.orderflow.infrastructure.persistence.mapper;

import com.camilagksantos.orderflow.domain.order.ShopOrder;
import com.camilagksantos.orderflow.domain.shared.Money;
import com.camilagksantos.orderflow.infrastructure.persistence.entity.ShopOrderEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

@Mapper(componentModel = "spring", uses = {OrderItemPersistenceMapper.class})
public interface ShopOrderPersistenceMapper {

    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "subtotal", source = "subtotal")
    @Mapping(target = "shippingCost", source = "shippingCost")
    @Mapping(target = "discountAmount", source = "discountAmount")
    @Mapping(target = "totalAmount", source = "totalAmount")
    ShopOrder toDomain(ShopOrderEntity entity);

    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "subtotal", source = "subtotal")
    @Mapping(target = "shippingCost", source = "shippingCost")
    @Mapping(target = "discountAmount", source = "discountAmount")
    @Mapping(target = "totalAmount", source = "totalAmount")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ShopOrderEntity toEntity(ShopOrder domain);
}