package com.camilagksantos.orderflow.infrastructure.persistence.mapper;

import com.camilagksantos.orderflow.domain.auth.User;
import com.camilagksantos.orderflow.infrastructure.persistence.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {RolePersistenceMapper.class})
public interface UserPersistenceMapper {

    User toDomain(UserEntity entity);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    UserEntity toEntity(User domain);
}