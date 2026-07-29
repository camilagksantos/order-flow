package com.camilagksantos.orderflow.application.service;

import com.camilagksantos.orderflow.application.port.output.CustomerRepositoryPort;
import com.camilagksantos.orderflow.domain.customer.Customer;
import com.camilagksantos.orderflow.domain.customer.CustomerStatus;
import com.camilagksantos.orderflow.domain.exception.CustomerNotFoundException;
import com.camilagksantos.orderflow.domain.shared.Email;
import com.camilagksantos.orderflow.domain.shared.NIF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepositoryPort customerRepositoryPort;

    @InjectMocks
    private CustomerService customerService;

    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .id(1L)
                .userId(1L)
                .name("Camila Kfouri")
                .email(new Email("camila@test.com"))
                .nif(new NIF("123456789"))
                .status(CustomerStatus.ACTIVE)
                .addresses(List.of())
                .build();
    }

    @Test
    void shouldRegisterCustomer() {
        when(customerRepositoryPort.save(customer)).thenReturn(customer);
        Customer registered = customerService.registerCustomer(customer);
        assertThat(registered.getName()).isEqualTo("Camila Kfouri");
        verify(customerRepositoryPort).save(customer);
    }

    @Test
    void shouldFindCustomerById() {
        when(customerRepositoryPort.findById(1L)).thenReturn(Optional.of(customer));
        Customer found = customerService.findCustomerById(1L);
        assertThat(found.getName()).isEqualTo("Camila Kfouri");
    }

    @Test
    void shouldThrowWhenCustomerNotFound() {
        when(customerRepositoryPort.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> customerService.findCustomerById(99L))
                .isInstanceOf(CustomerNotFoundException.class);
    }
}