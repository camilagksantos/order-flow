package com.camilagksantos.orderflow.infrastructure.adapter.output.persistence;

import com.camilagksantos.orderflow.application.port.output.ProcessedEventRepositoryPort;
import com.camilagksantos.orderflow.infrastructure.persistence.entity.ProcessedEventEntity;
import com.camilagksantos.orderflow.infrastructure.persistence.repository.ProcessedEventJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProcessedEventJpaAdapter implements ProcessedEventRepositoryPort {

    private final ProcessedEventJpaRepository processedEventJpaRepository;

    @Override
    public boolean existsById(String id) {
        return processedEventJpaRepository.existsById(id);
    }

    @Override
    public void save(String id) {
        ProcessedEventEntity entity = new ProcessedEventEntity();
        entity.setId(id);
        processedEventJpaRepository.save(entity);
    }
}