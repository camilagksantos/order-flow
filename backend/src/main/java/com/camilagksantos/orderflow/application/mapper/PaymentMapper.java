package com.camilagksantos.orderflow.application.mapper;

import com.camilagksantos.orderflow.application.dto.response.PaymentResponse;
import com.camilagksantos.orderflow.domain.payment.Payment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PaymentMapper {
    PaymentResponse toResponse(Payment domain);
}