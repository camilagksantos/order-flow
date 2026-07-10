package com.camilagksantos.orderflow.infrastructure.persistence.mapper;

import com.camilagksantos.orderflow.domain.product.Product;
import com.camilagksantos.orderflow.infrastructure.persistence.entity.ProductEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {CategoryPersistenceMapper.class})
public interface ProductPersistenceMapper {

    @Mapping(target = "price", expression = "java(com.camilagksantos.orderflow.domain.shared.Money.of(entity.getPrice()))")
    Product toDomain(ProductEntity entity);

    @Mapping(target = "price", expression = "java(domain.getPrice().amount())")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ProductEntity toEntity(Product domain);
}