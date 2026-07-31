package com.camilagksantos.orderflow.infrastructure.persistence.mapper;

import com.camilagksantos.orderflow.domain.cart.Cart;
import com.camilagksantos.orderflow.infrastructure.persistence.entity.CartEntity;
import com.camilagksantos.orderflow.infrastructure.persistence.entity.CustomerEntity;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {CartItemPersistenceMapper.class})
public interface CartPersistenceMapper {

    @Mapping(target = "customerId", source = "customer.id")
    Cart toDomain(CartEntity entity);

    @Mapping(target = "customer", source = "customerId")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    CartEntity toEntity(Cart domain);

    @AfterMapping
    default void linkItemsToCart(@MappingTarget CartEntity entity) {
        if (entity.getItems() != null) {
            entity.getItems().forEach(item -> item.setCart(entity));
        }
    }

    default CustomerEntity toCustomerEntity(Long customerId) {
        if (customerId == null) return null;
        CustomerEntity customer = new CustomerEntity();
        customer.setId(customerId);
        return customer;
    }

    default Long fromCustomerEntity(CustomerEntity customer) {
        return customer != null ? customer.getId() : null;
    }
}