package com.camilagksantos.orderflow.infrastructure.persistence.mapper;

import com.camilagksantos.orderflow.domain.cart.CartItem;
import com.camilagksantos.orderflow.domain.shared.Money;
import com.camilagksantos.orderflow.infrastructure.persistence.entity.CartItemEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface CartItemPersistenceMapper {

    @Mapping(target = "cartId", source = "cart.id")
    @Mapping(target = "unitPrice", source = "unitPrice")
    CartItem toDomain(CartItemEntity entity);

    @Mapping(target = "cart", ignore = true)
    @Mapping(target = "unitPrice", source = "unitPrice")
    CartItemEntity toEntity(CartItem domain);

    default Money toMoney(BigDecimal value) {
        return Money.of(value);
    }

    default BigDecimal toBigDecimal(Money money) {
        return money.amount();
    }
}