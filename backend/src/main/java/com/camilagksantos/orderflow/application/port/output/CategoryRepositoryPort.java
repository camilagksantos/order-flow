package com.camilagksantos.orderflow.application.port.output;

import com.camilagksantos.orderflow.domain.category.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepositoryPort {
    Category save(Category category);
    Optional<Category> findById(Long id);
    List<Category> findAll();
}
