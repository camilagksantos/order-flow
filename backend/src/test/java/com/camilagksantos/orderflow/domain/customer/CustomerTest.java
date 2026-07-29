package com.camilagksantos.orderflow.domain.customer;

import com.camilagksantos.orderflow.domain.shared.Email;
import com.camilagksantos.orderflow.domain.shared.NIF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class CustomerTest {

    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .id(1L)
                .userId(1L)
                .name("Camila Kfouri")
                .email(new Email("camila@test.com"))
                .nif(new NIF("123456789"))
                .phone("+351 912 345 678")
                .status(CustomerStatus.ACTIVE)
                .addresses(List.of())
                .build();
    }

    @Test
    void shouldBlockCustomer() {
        customer.block();
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.BLOCKED);
    }

    @Test
    void shouldActivateCustomer() {
        customer.block();
        customer.activate();
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
    }
}