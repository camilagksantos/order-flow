package com.camilagksantos.orderflow.infrastructure.persistence.mapper;

import com.camilagksantos.orderflow.domain.auth.Role;
import com.camilagksantos.orderflow.infrastructure.persistence.entity.RoleEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RolePersistenceMapper {
    Role toDomain(RoleEntity entity);
    RoleEntity toEntity(Role domain);
}