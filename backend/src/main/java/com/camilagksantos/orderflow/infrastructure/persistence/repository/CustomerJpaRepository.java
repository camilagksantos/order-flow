package com.camilagksantos.orderflow.infrastructure.persistence.repository;

import com.camilagksantos.orderflow.infrastructure.persistence.entity.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CustomerJpaRepository extends JpaRepository<CustomerEntity, Long> {
    Optional<CustomerEntity> findByEmail(String email);
    Optional<CustomerEntity> findByNif(String nif);
    @Query("select c from CustomerEntity c left join fetch c.addresses where c.id = :id")
    Optional<CustomerEntity> findByIdWithAddresses(Long id);
}