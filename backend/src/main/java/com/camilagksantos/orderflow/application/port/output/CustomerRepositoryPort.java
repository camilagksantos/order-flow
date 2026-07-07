package com.camilagksantos.orderflow.application.port.output;

import com.camilagksantos.orderflow.domain.customer.Customer;

import java.util.Optional;

public interface CustomerRepositoryPort {

    Customer save(Customer customer);
    Optional<Customer> findById(Long id);
    Optional<Customer> findByEmail(String email);
    Optional<Customer> findByNif(String nif);
}
