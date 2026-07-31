package com.camilagksantos.orderflow.infrastructure.adapter.output.persistence;

import com.camilagksantos.orderflow.application.port.output.RoleRepositoryPort;
import com.camilagksantos.orderflow.domain.auth.Role;
import com.camilagksantos.orderflow.infrastructure.persistence.entity.RoleEntity;
import com.camilagksantos.orderflow.infrastructure.persistence.mapper.RolePersistenceMapper;
import com.camilagksantos.orderflow.infrastructure.persistence.repository.RoleJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RoleJpaAdapter implements RoleRepositoryPort {

    private final RoleJpaRepository roleJpaRepository;
    private final RolePersistenceMapper rolePersistenceMapper;

    @Override
    public Optional<Role> findByName(String name) {
        return roleJpaRepository.findByName(name)
                .map(rolePersistenceMapper::toDomain);
    }

    @Override
    public Role save(Role role) {
        RoleEntity entity = rolePersistenceMapper.toEntity(role);
        return rolePersistenceMapper.toDomain(roleJpaRepository.save(entity));
    }
}