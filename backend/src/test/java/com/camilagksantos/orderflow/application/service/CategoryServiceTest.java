package com.camilagksantos.orderflow.application.service;

import com.camilagksantos.orderflow.application.port.output.CategoryRepositoryPort;
import com.camilagksantos.orderflow.domain.category.Category;
import com.camilagksantos.orderflow.domain.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepositoryPort categoryRepositoryPort;

    @InjectMocks
    private CategoryService categoryService;

    private Category category;

    @BeforeEach
    void setUp() {
        category = Category.builder()
                .id(1L)
                .name("Electronics")
                .build();
    }

    @Test
    void shouldCreateCategory() {
        when(categoryRepositoryPort.save(category)).thenReturn(category);
        Category created = categoryService.createCategory(category);
        assertThat(created.getName()).isEqualTo("Electronics");
        verify(categoryRepositoryPort).save(category);
    }

    @Test
    void shouldFindCategoryById() {
        when(categoryRepositoryPort.findById(1L)).thenReturn(Optional.of(category));
        Category found = categoryService.findCategoryById(1L);
        assertThat(found.getName()).isEqualTo("Electronics");
    }

    @Test
    void shouldThrowWhenCategoryNotFound() {
        when(categoryRepositoryPort.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> categoryService.findCategoryById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldFindAllCategories() {
        when(categoryRepositoryPort.findAll()).thenReturn(List.of(category));
        List<Category> categories = categoryService.findAllCategories();
        assertThat(categories).hasSize(1);
    }
}