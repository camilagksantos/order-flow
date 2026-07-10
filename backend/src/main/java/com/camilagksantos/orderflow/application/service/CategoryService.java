package com.camilagksantos.orderflow.application.service;

import com.camilagksantos.orderflow.application.port.input.CreateCategoryUseCase;
import com.camilagksantos.orderflow.application.port.input.FindCategoryUseCase;
import com.camilagksantos.orderflow.application.port.output.CategoryRepositoryPort;
import com.camilagksantos.orderflow.domain.exception.ResourceNotFoundException;
import com.camilagksantos.orderflow.domain.category.Category;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService implements CreateCategoryUseCase, FindCategoryUseCase {

    private final CategoryRepositoryPort categoryRepositoryPort;

    @Override
    public Category createCategory(Category category) {
        return categoryRepositoryPort.save(category);
    }

    @Override
    public Category findCategoryById(Long id) {
        return categoryRepositoryPort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
    }

    @Override
    public List<Category> findAllCategories() {
        return categoryRepositoryPort.findAll();
    }
}