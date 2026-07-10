package com.camilagksantos.orderflow.infrastructure.adapter.output.persistence;

import com.camilagksantos.orderflow.application.port.output.PaymentRepositoryPort;
import com.camilagksantos.orderflow.domain.payment.Payment;
import com.camilagksantos.orderflow.infrastructure.persistence.entity.PaymentEntity;
import com.camilagksantos.orderflow.infrastructure.persistence.mapper.PaymentPersistenceMapper;
import com.camilagksantos.orderflow.infrastructure.persistence.repository.PaymentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PaymentJpaAdapter implements PaymentRepositoryPort {

    private final PaymentJpaRepository paymentJpaRepository;
    private final PaymentPersistenceMapper paymentPersistenceMapper;

    @Override
    public Payment save(Payment payment) {
        PaymentEntity entity = paymentPersistenceMapper.toEntity(payment);
        return paymentPersistenceMapper.toDomain(paymentJpaRepository.save(entity));
    }

    @Override
    public Optional<Payment> findByOrderId(String orderId) {
        return paymentJpaRepository.findByOrderId(orderId)
                .map(paymentPersistenceMapper::toDomain);
    }
}