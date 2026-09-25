package com.camilagksantos.orderflow.infrastructure.persistence.repository;

import com.camilagksantos.orderflow.infrastructure.persistence.entity.ShopOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ShopOrderJpaRepository extends JpaRepository<ShopOrderEntity, String> {
    Optional<ShopOrderEntity> findByOrderNumber(String orderNumber);
    Optional<ShopOrderEntity> findByIdempotencyKey(String idempotencyKey);
    List<ShopOrderEntity> findByCustomerId(Long customerId);
    @Query("select o from ShopOrderEntity o left join fetch o.items where o.id = :id")
    Optional<ShopOrderEntity> findByIdWithItems(String id);
    @Query("select o from ShopOrderEntity o left join fetch o.items where o.createdAt >= :start and o.createdAt < :end")
    List<ShopOrderEntity> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}