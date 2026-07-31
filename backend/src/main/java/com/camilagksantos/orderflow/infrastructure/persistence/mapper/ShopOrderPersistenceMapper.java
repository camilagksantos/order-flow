package com.camilagksantos.orderflow.infrastructure.persistence.mapper;

import com.camilagksantos.orderflow.domain.order.ShopOrder;
import com.camilagksantos.orderflow.infrastructure.persistence.entity.CustomerEntity;
import com.camilagksantos.orderflow.infrastructure.persistence.entity.ShopOrderEntity;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {OrderItemPersistenceMapper.class})
public interface ShopOrderPersistenceMapper {

    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "subtotal", source = "subtotal")
    @Mapping(target = "shippingCost", source = "shippingCost")
    @Mapping(target = "discountAmount", source = "discountAmount")
    @Mapping(target = "totalAmount", source = "totalAmount")
    ShopOrder toDomain(ShopOrderEntity entity);

    @Mapping(target = "customer", source = "customerId")
    @Mapping(target = "subtotal", source = "subtotal")
    @Mapping(target = "shippingCost", source = "shippingCost")
    @Mapping(target = "discountAmount", source = "discountAmount")
    @Mapping(target = "totalAmount", source = "totalAmount")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ShopOrderEntity toEntity(ShopOrder domain);

    @AfterMapping
    default void linkItemsToOrder(@MappingTarget ShopOrderEntity entity) {
        if (entity.getItems() != null) {
            entity.getItems().forEach(item -> item.setOrder(entity));
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