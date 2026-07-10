package com.camilagksantos.orderflow.infrastructure.persistence.mapper;

import com.camilagksantos.orderflow.domain.cart.CartItem;
import com.camilagksantos.orderflow.infrastructure.persistence.entity.CartItemEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CartItemPersistenceMapper {

    @Mapping(target = "cartId", source = "cart.id")
    @Mapping(target = "unitPrice", expression = "java(com.camilagksantos.orderflow.domain.shared.Money.of(entity.getUnitPrice()))")
    CartItem toDomain(CartItemEntity entity);

    @Mapping(target = "cart", ignore = true)
    @Mapping(target = "unitPrice", expression = "java(domain.getUnitPrice().amount())")
    CartItemEntity toEntity(CartItem domain);
}