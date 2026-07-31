package com.camilagksantos.orderflow.application.port.output;

import com.camilagksantos.orderflow.domain.auth.User;

import java.util.Optional;

public interface UserRepositoryPort {

    Optional<User> findByEmail(String email);

    User save(User user);
}