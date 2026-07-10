package com.camilagksantos.orderflow.infrastructure.persistence.mapper;

import com.camilagksantos.orderflow.domain.customer.Address;
import com.camilagksantos.orderflow.infrastructure.persistence.entity.AddressEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AddressPersistenceMapper {

    @Mapping(target = "customerId", source = "customer.id")
    Address toDomain(AddressEntity entity);

    @Mapping(target = "customer", ignore = true)
    AddressEntity toEntity(Address domain);
}