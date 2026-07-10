package com.camilagksantos.orderflow.infrastructure.persistence.mapper;

import com.camilagksantos.orderflow.domain.order.ShopOrder;
import com.camilagksantos.orderflow.infrastructure.persistence.entity.ShopOrderEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {OrderItemPersistenceMapper.class})
public interface ShopOrderPersistenceMapper {

    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "subtotal", expression = "java(com.camilagksantos.orderflow.domain.shared.Money.of(entity.getSubtotal()))")
    @Mapping(target = "shippingCost", expression = "java(com.camilagksantos.orderflow.domain.shared.Money.of(entity.getShippingCost()))")
    @Mapping(target = "discountAmount", expression = "java(com.camilagksantos.orderflow.domain.shared.Money.of(entity.getDiscountAmount()))")
    @Mapping(target = "totalAmount", expression = "java(com.camilagksantos.orderflow.domain.shared.Money.of(entity.getTotalAmount()))")
    ShopOrder toDomain(ShopOrderEntity entity);

    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "subtotal", expression = "java(domain.getSubtotal().amount())")
    @Mapping(target = "shippingCost", expression = "java(domain.getShippingCost().amount())")
    @Mapping(target = "discountAmount", expression = "java(domain.getDiscountAmount().amount())")
    @Mapping(target = "totalAmount", expression = "java(domain.getTotalAmount().amount())")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ShopOrderEntity toEntity(ShopOrder domain);
}