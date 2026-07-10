package com.camilagksantos.orderflow.infrastructure.persistence.repository;

import com.camilagksantos.orderflow.infrastructure.persistence.entity.ShopOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ShopOrderJpaRepository extends JpaRepository<ShopOrderEntity, String> {
    Optional<ShopOrderEntity> findByOrderNumber(String orderNumber);
    Optional<ShopOrderEntity> findByIdempotencyKey(String idempotencyKey);
    List<ShopOrderEntity> findByCustomerId(Long customerId);
}