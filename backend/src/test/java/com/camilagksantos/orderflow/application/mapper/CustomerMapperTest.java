package com.camilagksantos.orderflow.application.mapper;

import com.camilagksantos.orderflow.application.dto.request.RegisterCustomerRequest;
import com.camilagksantos.orderflow.application.dto.response.CustomerResponse;
import com.camilagksantos.orderflow.domain.customer.Customer;
import com.camilagksantos.orderflow.domain.customer.CustomerStatus;
import com.camilagksantos.orderflow.domain.shared.Email;
import com.camilagksantos.orderflow.domain.shared.NIF;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class CustomerMapperTest {

    private final AddressMapper addressMapper = new AddressMapperImpl();
    private final CustomerMapper customerMapper;

    CustomerMapperTest() throws Exception {
        CustomerMapperImpl impl = new CustomerMapperImpl();
        var field = CustomerMapperImpl.class.getDeclaredField("addressMapper");
        field.setAccessible(true);
        field.set(impl, addressMapper);
        this.customerMapper = impl;
    }

    @Test
    void shouldMapCustomerToResponse() {
        Customer customer = Customer.builder()
                .id(1L)
                .name("Camila Kfouri")
                .email(new Email("camila@test.com"))
                .nif(new NIF("123456789"))
                .phone("+351 912 345 678")
                .status(CustomerStatus.ACTIVE)
                .addresses(List.of())
                .build();

        CustomerResponse response = customerMapper.toResponse(customer);

        assertThat(response.name()).isEqualTo("Camila Kfouri");
        assertThat(response.email()).isEqualTo("camila@test.com");
        assertThat(response.nif()).isEqualTo("123456789");
    }

    @Test
    void shouldMapRegisterRequestToDomain() {
        RegisterCustomerRequest request = new RegisterCustomerRequest(
                "Camila Kfouri", "camila@test.com", "123456789", "+351 912 345 678"
        );

        Customer customer = customerMapper.toDomain(request);

        assertThat(customer.getName()).isEqualTo("Camila Kfouri");
        assertThat(customer.getEmail().value()).isEqualTo("camila@test.com");
        assertThat(customer.getNif().value()).isEqualTo("123456789");
        assertThat(customer.getId()).isNull();
    }
}