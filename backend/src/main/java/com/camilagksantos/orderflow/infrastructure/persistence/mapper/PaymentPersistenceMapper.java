package com.camilagksantos.orderflow.infrastructure.persistence.mapper;

import com.camilagksantos.orderflow.domain.payment.Payment;
import com.camilagksantos.orderflow.domain.shared.Money;
import com.camilagksantos.orderflow.infrastructure.persistence.entity.PaymentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface PaymentPersistenceMapper {

    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "amount", source = "amount")
    Payment toDomain(PaymentEntity entity);

    @Mapping(target = "order", ignore = true)
    @Mapping(target = "amount", source = "amount")
    @Mapping(target = "createdAt", ignore = true)
    PaymentEntity toEntity(Payment domain);

    default Money toMoney(BigDecimal value) {
        return Money.of(value);
    }

    default BigDecimal toBigDecimal(Money money) {
        return money.amount();
    }
}