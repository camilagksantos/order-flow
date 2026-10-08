package com.camilagksantos.orderflow.application.service;

import com.camilagksantos.orderflow.application.port.input.AddAddressUseCase;
import com.camilagksantos.orderflow.application.port.input.FindCustomerUseCase;
import com.camilagksantos.orderflow.application.port.input.RegisterCustomerUseCase;
import com.camilagksantos.orderflow.application.port.input.RemoveAddressUseCase;
import com.camilagksantos.orderflow.application.port.input.SetDefaultAddressUseCase;
import com.camilagksantos.orderflow.application.port.input.UpdateAddressUseCase;
import com.camilagksantos.orderflow.application.port.output.CustomerRepositoryPort;
import com.camilagksantos.orderflow.application.port.output.RoleRepositoryPort;
import com.camilagksantos.orderflow.application.port.output.UserRepositoryPort;
import com.camilagksantos.orderflow.domain.auth.Role;
import com.camilagksantos.orderflow.domain.auth.User;
import com.camilagksantos.orderflow.domain.customer.Address;
import com.camilagksantos.orderflow.domain.customer.Customer;
import com.camilagksantos.orderflow.domain.exception.CustomerNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService implements RegisterCustomerUseCase, FindCustomerUseCase,
        AddAddressUseCase, UpdateAddressUseCase, SetDefaultAddressUseCase, RemoveAddressUseCase {

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

    @Override
    @Transactional
    public Address addAddress(Long customerId, Address address) {
        Customer customer = findCustomerById(customerId);
        customer.addAddress(address);
        Customer saved = customerRepositoryPort.save(customer);
        return saved.getAddresses().stream()
                .max(Comparator.comparing(Address::getId))
                .orElseThrow();
    }

    @Override
    @Transactional
    public Address updateAddress(Long customerId, Long addressId, Address data) {
        Customer customer = findCustomerById(customerId);
        customer.updateAddress(addressId, data);
        Customer saved = customerRepositoryPort.save(customer);
        return saved.findAddress(addressId);
    }

    @Override
    @Transactional
    public Address setDefaultAddress(Long customerId, Long addressId) {
        Customer customer = findCustomerById(customerId);
        customer.makeAddressDefault(addressId);
        Customer saved = customerRepositoryPort.save(customer);
        return saved.findAddress(addressId);
    }

    @Override
    @Transactional
    public void removeAddress(Long customerId, Long addressId) {
        Customer customer = findCustomerById(customerId);
        customer.removeAddress(addressId);
        customerRepositoryPort.save(customer);
    }
}