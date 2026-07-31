package com.camilagksantos.orderflow.application.port.output;

import com.camilagksantos.orderflow.domain.auth.Role;

import java.util.Optional;

public interface RoleRepositoryPort {

    Optional<Role> findByName(String name);

    Role save(Role role);
}