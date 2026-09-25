package com.camilagksantos.orderflow.infrastructure.adapter.output.persistence;

import com.camilagksantos.orderflow.application.port.output.OrderRepositoryPort;
import com.camilagksantos.orderflow.domain.order.ShopOrder;
import com.camilagksantos.orderflow.infrastructure.persistence.entity.ShopOrderEntity;
import com.camilagksantos.orderflow.infrastructure.persistence.mapper.ShopOrderPersistenceMapper;
import com.camilagksantos.orderflow.infrastructure.persistence.repository.ShopOrderJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OrderJpaAdapter implements OrderRepositoryPort {

    private final ShopOrderJpaRepository shopOrderJpaRepository;
    private final ShopOrderPersistenceMapper shopOrderPersistenceMapper;

    @Override
    public ShopOrder save(ShopOrder order) {
        ShopOrderEntity entity = shopOrderPersistenceMapper.toEntity(order);
        ShopOrderEntity saved = shopOrderJpaRepository.save(entity);
        return shopOrderPersistenceMapper.toDomain(
                shopOrderJpaRepository.findByIdWithItems(saved.getId()).orElseThrow()
        );
    }

    @Override
    public Optional<ShopOrder> findById(String id) {
        return shopOrderJpaRepository.findByIdWithItems(id)
                .map(shopOrderPersistenceMapper::toDomain);
    }

    @Override
    public Optional<ShopOrder> findByOrderNumber(String orderNumber) {
        return shopOrderJpaRepository.findByOrderNumber(orderNumber)
                .map(shopOrderPersistenceMapper::toDomain);
    }

    @Override
    public Optional<ShopOrder> findByIdempotencyKey(String idempotencyKey) {
        return shopOrderJpaRepository.findByIdempotencyKey(idempotencyKey)
                .map(shopOrderPersistenceMapper::toDomain);
    }

    @Override
    public List<ShopOrder> findByCustomerId(Long customerId) {
        return shopOrderJpaRepository.findByCustomerId(customerId).stream()
                .map(shopOrderPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<ShopOrder> findByCreatedAtBetween(LocalDate startDate, LocalDate endDate) {
        return shopOrderJpaRepository.findByCreatedAtBetween(
                        startDate.atStartOfDay(),
                        endDate.plusDays(1).atStartOfDay()
                ).stream()
                .map(shopOrderPersistenceMapper::toDomain)
                .toList();
    }
}