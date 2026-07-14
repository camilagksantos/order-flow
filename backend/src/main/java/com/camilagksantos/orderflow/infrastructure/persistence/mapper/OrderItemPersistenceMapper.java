package com.camilagksantos.orderflow.infrastructure.persistence.mapper;

import com.camilagksantos.orderflow.domain.order.OrderItem;
import com.camilagksantos.orderflow.domain.shared.Money;
import com.camilagksantos.orderflow.infrastructure.persistence.entity.OrderItemEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface OrderItemPersistenceMapper {

    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "unitPrice", source = "unitPrice")
    @Mapping(target = "subtotal", source = "subtotal")
    OrderItem toDomain(OrderItemEntity entity);

    @Mapping(target = "order", ignore = true)
    @Mapping(target = "unitPrice", source = "unitPrice")
    @Mapping(target = "subtotal", source = "subtotal")
    OrderItemEntity toEntity(OrderItem domain);

    default Money toMoney(BigDecimal value) {
        return Money.of(value);
    }

    default BigDecimal toBigDecimal(Money money) {
        return money.amount();
    }
}