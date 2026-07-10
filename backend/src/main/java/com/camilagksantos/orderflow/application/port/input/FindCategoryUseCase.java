package com.camilagksantos.orderflow.application.port.input;

import com.camilagksantos.orderflow.domain.category.Category;

import java.util.List;

public interface FindCategoryUseCase {

    Category findCategoryById(Long id);
    List<Category> findAllCategories();
}
