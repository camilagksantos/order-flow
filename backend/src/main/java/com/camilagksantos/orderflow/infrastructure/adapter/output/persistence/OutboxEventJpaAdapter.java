package com.camilagksantos.orderflow.infrastructure.adapter.output.persistence;

import com.camilagksantos.orderflow.application.port.output.OutboxEventRepositoryPort;
import com.camilagksantos.orderflow.domain.event.OutboxEvent;
import com.camilagksantos.orderflow.domain.event.OutboxEventStatus;
import com.camilagksantos.orderflow.infrastructure.persistence.entity.OutboxEventEntity;
import com.camilagksantos.orderflow.infrastructure.persistence.mapper.OutboxEventPersistenceMapper;
import com.camilagksantos.orderflow.infrastructure.persistence.repository.OutboxEventJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OutboxEventJpaAdapter implements OutboxEventRepositoryPort {

    private final OutboxEventJpaRepository outboxEventJpaRepository;
    private final OutboxEventPersistenceMapper outboxEventPersistenceMapper;

    @Override
    public OutboxEvent save(OutboxEvent event) {
        OutboxEventEntity entity = outboxEventPersistenceMapper.toEntity(event);
        return outboxEventPersistenceMapper.toDomain(outboxEventJpaRepository.save(entity));
    }

    @Override
    public List<OutboxEvent> findByStatus(OutboxEventStatus status) {
        return outboxEventJpaRepository.findByStatus(status).stream()
                .map(outboxEventPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public OutboxEvent updateStatus(String id, OutboxEventStatus status) {
        OutboxEventEntity entity = outboxEventJpaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("OutboxEvent not found: " + id));
        entity.setStatus(status);
        return outboxEventPersistenceMapper.toDomain(outboxEventJpaRepository.save(entity));
    }
}