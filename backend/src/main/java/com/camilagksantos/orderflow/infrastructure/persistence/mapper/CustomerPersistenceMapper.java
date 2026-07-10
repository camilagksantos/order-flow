package com.camilagksantos.orderflow.infrastructure.persistence.mapper;

import com.camilagksantos.orderflow.domain.customer.Customer;
import com.camilagksantos.orderflow.infrastructure.persistence.entity.CustomerEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {AddressPersistenceMapper.class})
public interface CustomerPersistenceMapper {

    @Mapping(target = "email", expression = "java(new com.camilagksantos.orderflow.domain.shared.Email(entity.getEmail()))")
    @Mapping(target = "nif", expression = "java(new com.camilagksantos.orderflow.domain.shared.NIF(entity.getNif()))")
    @Mapping(target = "userId", source = "user.id")
    Customer toDomain(CustomerEntity entity);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "email", expression = "java(domain.getEmail().value())")
    @Mapping(target = "nif", expression = "java(domain.getNif().value())")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    CustomerEntity toEntity(Customer domain);
}