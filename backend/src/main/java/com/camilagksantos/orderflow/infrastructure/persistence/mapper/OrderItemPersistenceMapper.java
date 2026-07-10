package com.camilagksantos.orderflow.infrastructure.persistence.mapper;

import com.camilagksantos.orderflow.domain.order.OrderItem;
import com.camilagksantos.orderflow.infrastructure.persistence.entity.OrderItemEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderItemPersistenceMapper {

    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "unitPrice", expression = "java(com.camilagksantos.orderflow.domain.shared.Money.of(entity.getUnitPrice()))")
    OrderItem toDomain(OrderItemEntity entity);

    @Mapping(target = "order", ignore = true)
    @Mapping(target = "unitPrice", expression = "java(domain.getUnitPrice().amount())")
    @Mapping(target = "subtotal", expression = "java(domain.subtotal().amount())")
    OrderItemEntity toEntity(OrderItem domain);
}