package com.camilagksantos.orderflow.infrastructure.adapter.output.persistence;

import com.camilagksantos.orderflow.application.port.output.CustomerRepositoryPort;
import com.camilagksantos.orderflow.domain.customer.Customer;
import com.camilagksantos.orderflow.infrastructure.persistence.entity.CustomerEntity;
import com.camilagksantos.orderflow.infrastructure.persistence.mapper.CustomerPersistenceMapper;
import com.camilagksantos.orderflow.infrastructure.persistence.repository.CustomerJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CustomerJpaAdapter implements CustomerRepositoryPort {

    private final CustomerJpaRepository customerJpaRepository;
    private final CustomerPersistenceMapper customerPersistenceMapper;

    @Override
    public Customer save(Customer customer) {
        CustomerEntity entity = customerPersistenceMapper.toEntity(customer);
        return customerPersistenceMapper.toDomain(customerJpaRepository.save(entity));
    }

    @Override
    public Optional<Customer> findById(Long id) {
        return customerJpaRepository.findById(id)
                .map(customerPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Customer> findByEmail(String email) {
        return customerJpaRepository.findByEmail(email)
                .map(customerPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Customer> findByNif(String nif) {
        return customerJpaRepository.findByNif(nif)
                .map(customerPersistenceMapper::toDomain);
    }
}