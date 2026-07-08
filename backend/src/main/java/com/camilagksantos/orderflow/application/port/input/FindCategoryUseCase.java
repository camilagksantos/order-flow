package com.camilagksantos.orderflow.application.port.input;

import com.camilagksantos.orderflow.domain.category.Category;

import java.util.List;

public interface FindCategoryUseCase {

    Category findCategory(Long id);
    List<Category> findAll();
}
