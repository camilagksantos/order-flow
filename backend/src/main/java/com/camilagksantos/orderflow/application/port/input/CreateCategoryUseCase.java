package com.camilagksantos.orderflow.application.port.input;

import com.camilagksantos.orderflow.domain.category.Category;

public interface CreateCategoryUseCase {

    Category createCategory(Category category);
}
