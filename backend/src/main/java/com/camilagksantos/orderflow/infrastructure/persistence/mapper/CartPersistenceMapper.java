package com.camilagksantos.orderflow.infrastructure.persistence.mapper;

import com.camilagksantos.orderflow.domain.cart.Cart;
import com.camilagksantos.orderflow.infrastructure.persistence.entity.CartEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {CartItemPersistenceMapper.class})
public interface CartPersistenceMapper {

    @Mapping(target = "customerId", source = "customer.id")
    Cart toDomain(CartEntity entity);

    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    CartEntity toEntity(Cart domain);
}