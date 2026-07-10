package com.camilagksantos.orderflow.infrastructure.persistence.mapper;

import com.camilagksantos.orderflow.domain.payment.Payment;
import com.camilagksantos.orderflow.infrastructure.persistence.entity.PaymentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentPersistenceMapper {

    @Mapping(target = "orderId", source = "order.id")
    @Mapping(target = "amount", expression = "java(com.camilagksantos.orderflow.domain.shared.Money.of(entity.getAmount()))")
    Payment toDomain(PaymentEntity entity);

    @Mapping(target = "order", ignore = true)
    @Mapping(target = "amount", expression = "java(domain.getAmount().amount())")
    @Mapping(target = "createdAt", ignore = true)
    PaymentEntity toEntity(Payment domain);
}