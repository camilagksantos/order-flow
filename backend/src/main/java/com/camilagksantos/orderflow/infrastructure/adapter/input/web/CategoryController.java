package com.camilagksantos.orderflow.infrastructure.adapter.input.web;

import com.camilagksantos.orderflow.application.dto.request.CreateCategoryRequest;
import com.camilagksantos.orderflow.application.dto.response.CategoryResponse;
import com.camilagksantos.orderflow.application.mapper.CategoryMapper;
import com.camilagksantos.orderflow.application.port.input.CreateCategoryUseCase;
import com.camilagksantos.orderflow.application.port.input.FindCategoryUseCase;
import com.camilagksantos.orderflow.domain.category.Category;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CreateCategoryUseCase createCategoryUseCase;
    private final FindCategoryUseCase findCategoryUseCase;
    private final CategoryMapper categoryMapper;

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CreateCategoryRequest request) {
        Category category = categoryMapper.toDomain(request);
        Category created = createCategoryUseCase.createCategory(category);
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryMapper.toResponse(created));
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> findAll() {
        List<CategoryResponse> categories = findCategoryUseCase.findAllCategories().stream()
                .map(categoryMapper::toResponse)
                .toList();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryMapper.toResponse(findCategoryUseCase.findCategoryById(id)));
    }
}