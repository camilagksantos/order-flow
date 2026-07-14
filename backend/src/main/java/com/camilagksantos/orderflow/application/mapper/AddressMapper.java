package com.camilagksantos.orderflow.application.mapper;

import com.camilagksantos.orderflow.application.dto.request.CreateAddressRequest;
import com.camilagksantos.orderflow.application.dto.response.AddressResponse;
import com.camilagksantos.orderflow.domain.customer.Address;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AddressMapper {

    AddressResponse toResponse(Address domain);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customerId", ignore = true)
    @Mapping(target = "country", constant = "PT")
    @Mapping(target = "defaultAddress", constant = "false")
    Address toDomain(CreateAddressRequest request);
}