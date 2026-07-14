package com.camilagksantos.orderflow.infrastructure.persistence.mapper;

import com.camilagksantos.orderflow.domain.product.Product;
import com.camilagksantos.orderflow.domain.shared.Money;
import com.camilagksantos.orderflow.infrastructure.persistence.entity.ProductEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

@Mapper(componentModel = "spring", uses = {CategoryPersistenceMapper.class})
public interface ProductPersistenceMapper {

    @Mapping(target = "price", source = "price")
    Product toDomain(ProductEntity entity);

    @Mapping(target = "price", source = "price")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ProductEntity toEntity(Product domain);

    default Money toMoney(BigDecimal value) {
        return Money.of(value);
    }

    default BigDecimal toBigDecimal(Money money) {
        return money.amount();
    }
}