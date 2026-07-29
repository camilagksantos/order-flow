package com.camilagksantos.orderflow.application.mapper;

import com.camilagksantos.orderflow.application.dto.request.CreateCategoryRequest;
import com.camilagksantos.orderflow.application.dto.response.CategoryResponse;
import com.camilagksantos.orderflow.domain.category.Category;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class CategoryMapperTest {

    private final CategoryMapper categoryMapper = new CategoryMapperImpl();

    @Test
    void shouldMapCategoryToResponse() {
        Category category = Category.builder()
                .id(1L)
                .name("Electronics")
                .build();

        CategoryResponse response = categoryMapper.toResponse(category);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Electronics");
    }

    @Test
    void shouldMapRequestToDomain() {
        CreateCategoryRequest request = new CreateCategoryRequest("Electronics");

        Category category = categoryMapper.toDomain(request);

        assertThat(category.getName()).isEqualTo("Electronics");
        assertThat(category.getId()).isNull();
    }
}