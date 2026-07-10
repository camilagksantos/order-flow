package com.camilagksantos.orderflow.infrastructure.adapter.output.persistence;

import com.camilagksantos.orderflow.application.port.output.CategoryRepositoryPort;
import com.camilagksantos.orderflow.domain.category.Category;
import com.camilagksantos.orderflow.infrastructure.persistence.entity.CategoryEntity;
import com.camilagksantos.orderflow.infrastructure.persistence.mapper.CategoryPersistenceMapper;
import com.camilagksantos.orderflow.infrastructure.persistence.repository.CategoryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CategoryJpaAdapter implements CategoryRepositoryPort {

    private final CategoryJpaRepository categoryJpaRepository;
    private final CategoryPersistenceMapper categoryPersistenceMapper;

    @Override
    public Category save(Category category) {
        CategoryEntity entity = categoryPersistenceMapper.toEntity(category);
        return categoryPersistenceMapper.toDomain(categoryJpaRepository.save(entity));
    }

    @Override
    public Optional<Category> findById(Long id) {
        return categoryJpaRepository.findById(id)
                .map(categoryPersistenceMapper::toDomain);
    }

    @Override
    public List<Category> findAll() {
        return categoryJpaRepository.findAll().stream()
                .map(categoryPersistenceMapper::toDomain)
                .toList();
    }
}