package com.camilagksantos.orderflow.application.service;

import com.camilagksantos.orderflow.application.port.output.CustomerRepositoryPort;
import com.camilagksantos.orderflow.application.port.output.RoleRepositoryPort;
import com.camilagksantos.orderflow.application.port.output.UserRepositoryPort;
import com.camilagksantos.orderflow.domain.auth.Role;
import com.camilagksantos.orderflow.domain.auth.User;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepositoryPort customerRepositoryPort;

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private RoleRepositoryPort roleRepositoryPort;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CustomerService customerService;

    private Customer customer;
    private Role customerRole;
    private User savedUser;

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

        customerRole = Role.builder()
                .id(1L)
                .name("CUSTOMER")
                .build();

        savedUser = User.builder()
                .id(1L)
                .email("camila@test.com")
                .password("hashed-password")
                .active(true)
                .roles(List.of(customerRole))
                .build();
    }

    @Test
    void shouldRegisterCustomer() {
        when(roleRepositoryPort.findByName("CUSTOMER")).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode("rawPassword123")).thenReturn("hashed-password");
        when(userRepositoryPort.save(any())).thenReturn(savedUser);
        when(customerRepositoryPort.save(any())).thenReturn(customer);

        Customer registered = customerService.registerCustomer(customer, "rawPassword123");

        assertThat(registered.getName()).isEqualTo("Camila Kfouri");
        verify(userRepositoryPort).save(any());
        verify(customerRepositoryPort).save(customer);
    }

    @Test
    void shouldThrowWhenCustomerRoleNotFound() {
        when(roleRepositoryPort.findByName("CUSTOMER")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.registerCustomer(customer, "rawPassword123"))
                .isInstanceOf(IllegalStateException.class);

        verify(userRepositoryPort, never()).save(any());
        verify(customerRepositoryPort, never()).save(any());
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