package com.camilagksantos.orderflow.infrastructure.persistence.mapper;

import com.camilagksantos.orderflow.domain.event.OutboxEvent;
import com.camilagksantos.orderflow.infrastructure.persistence.entity.OutboxEventEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OutboxEventPersistenceMapper {

    OutboxEvent toDomain(OutboxEventEntity entity);

    @Mapping(target = "createdAt", ignore = true)
    OutboxEventEntity toEntity(OutboxEvent domain);
}