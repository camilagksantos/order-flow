package com.camilagksantos.orderflow.application.service;

import com.camilagksantos.orderflow.application.port.input.FindCustomerUseCase;
import com.camilagksantos.orderflow.application.port.input.RegisterCustomerUseCase;
import com.camilagksantos.orderflow.application.port.output.CustomerRepositoryPort;
import com.camilagksantos.orderflow.domain.customer.Customer;
import com.camilagksantos.orderflow.domain.exception.CustomerNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerService implements RegisterCustomerUseCase, FindCustomerUseCase {

    private final CustomerRepositoryPort customerRepositoryPort;

    @Override
    public Customer registerCustomer(Customer customer) {
        return customerRepositoryPort.save(customer);
    }

    @Override
    public Customer findCustomerById(Long id) {
        return customerRepositoryPort.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));
    }
}