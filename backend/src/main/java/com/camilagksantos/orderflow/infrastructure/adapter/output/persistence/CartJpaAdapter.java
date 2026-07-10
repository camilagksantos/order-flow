package com.camilagksantos.orderflow.infrastructure.adapter.output.persistence;

import com.camilagksantos.orderflow.application.port.output.CartRepositoryPort;
import com.camilagksantos.orderflow.domain.cart.Cart;
import com.camilagksantos.orderflow.domain.cart.CartStatus;
import com.camilagksantos.orderflow.infrastructure.persistence.entity.CartEntity;
import com.camilagksantos.orderflow.infrastructure.persistence.mapper.CartPersistenceMapper;
import com.camilagksantos.orderflow.infrastructure.persistence.repository.CartJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CartJpaAdapter implements CartRepositoryPort {

    private final CartJpaRepository cartJpaRepository;
    private final CartPersistenceMapper cartPersistenceMapper;

    @Override
    public Cart save(Cart cart) {
        CartEntity entity = cartPersistenceMapper.toEntity(cart);
        return cartPersistenceMapper.toDomain(cartJpaRepository.save(entity));
    }

    @Override
    public Optional<Cart> findById(String id) {
        return cartJpaRepository.findById(id)
                .map(cartPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Cart> findActiveByCustomerId(Long customerId) {
        return cartJpaRepository.findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE)
                .map(cartPersistenceMapper::toDomain);
    }
}