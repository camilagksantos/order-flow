package com.camilagksantos.orderflow.application.mapper;

import com.camilagksantos.orderflow.application.dto.request.CreateCategoryRequest;
import com.camilagksantos.orderflow.application.dto.response.CategoryResponse;
import com.camilagksantos.orderflow.domain.category.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryResponse toResponse(Category domain);

    @Mapping(target = "id", ignore = true)
    Category toDomain(CreateCategoryRequest request);
}