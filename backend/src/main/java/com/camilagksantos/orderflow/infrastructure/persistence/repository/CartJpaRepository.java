package com.camilagksantos.orderflow.infrastructure.persistence.repository;

import com.camilagksantos.orderflow.infrastructure.persistence.entity.CartEntity;
import com.camilagksantos.orderflow.domain.cart.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CartJpaRepository extends JpaRepository<CartEntity, String> {

    @Query("select c from CartEntity c left join fetch c.items where c.customer.id = :customerId and c.status = :status")
    Optional<CartEntity> findByCustomerIdAndStatus(Long customerId, CartStatus status);

    @Query("select c from CartEntity c left join fetch c.items where c.id = :id")
    Optional<CartEntity> findByIdWithItems(String id);
}