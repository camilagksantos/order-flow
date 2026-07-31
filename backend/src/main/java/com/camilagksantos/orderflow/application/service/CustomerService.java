package com.camilagksantos.orderflow.application.service;

import com.camilagksantos.orderflow.application.port.input.FindCustomerUseCase;
import com.camilagksantos.orderflow.application.port.input.RegisterCustomerUseCase;
import com.camilagksantos.orderflow.application.port.output.CustomerRepositoryPort;
import com.camilagksantos.orderflow.application.port.output.RoleRepositoryPort;
import com.camilagksantos.orderflow.application.port.output.UserRepositoryPort;
import com.camilagksantos.orderflow.domain.auth.Role;
import com.camilagksantos.orderflow.domain.auth.User;
import com.camilagksantos.orderflow.domain.customer.Customer;
import com.camilagksantos.orderflow.domain.exception.CustomerNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService implements RegisterCustomerUseCase, FindCustomerUseCase {

    private static final String DEFAULT_ROLE = "CUSTOMER";

    private final CustomerRepositoryPort customerRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final RoleRepositoryPort roleRepositoryPort;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public Customer registerCustomer(Customer customer, String rawPassword) {
        Role customerRole = roleRepositoryPort.findByName(DEFAULT_ROLE)
                .orElseThrow(() -> new IllegalStateException("Role not found: " + DEFAULT_ROLE));

        User user = User.builder()
                .email(customer.getEmail().value())
                .password(passwordEncoder.encode(rawPassword))
                .active(true)
                .roles(List.of(customerRole))
                .build();

        User savedUser = userRepositoryPort.save(user);

        customer.setUserId(savedUser.getId());
        return customerRepositoryPort.save(customer);
    }

    @Override
    public Customer findCustomerById(Long id) {
        return customerRepositoryPort.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));
    }
}