package com.camilagksantos.orderflow.application.mapper;

import com.camilagksantos.orderflow.application.dto.request.RegisterCustomerRequest;
import com.camilagksantos.orderflow.application.dto.response.CustomerResponse;
import com.camilagksantos.orderflow.domain.customer.Customer;
import com.camilagksantos.orderflow.domain.shared.Email;
import com.camilagksantos.orderflow.domain.shared.NIF;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {AddressMapper.class})
public interface CustomerMapper {

    @Mapping(target = "email", source = "email")
    @Mapping(target = "nif", source = "nif")
    CustomerResponse toResponse(Customer domain);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "email", source = "email")
    @Mapping(target = "nif", source = "nif")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "addresses", ignore = true)
    Customer toDomain(RegisterCustomerRequest request);

    default Email toEmail(String value) {
        return new Email(value);
    }

    default String fromEmail(Email email) {
        return email.value();
    }

    default NIF toNIF(String value) {
        return new NIF(value);
    }

    default String fromNIF(NIF nif) {
        return nif.value();
    }
}