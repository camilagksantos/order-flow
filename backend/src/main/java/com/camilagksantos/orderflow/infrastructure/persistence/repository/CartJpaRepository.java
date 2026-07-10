package com.camilagksantos.orderflow.infrastructure.persistence.repository;

import com.camilagksantos.orderflow.infrastructure.persistence.entity.CartEntity;
import com.camilagksantos.orderflow.domain.cart.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartJpaRepository extends JpaRepository<CartEntity, String> {
    Optional<CartEntity> findByCustomerIdAndStatus(Long customerId, CartStatus status);
}