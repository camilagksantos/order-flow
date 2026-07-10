package com.camilagksantos.orderflow.infrastructure.persistence.mapper;

import com.camilagksantos.orderflow.domain.category.Category;
import com.camilagksantos.orderflow.infrastructure.persistence.entity.CategoryEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryPersistenceMapper {

    Category toDomain(CategoryEntity entity);
    CategoryEntity toEntity(Category domain);
}