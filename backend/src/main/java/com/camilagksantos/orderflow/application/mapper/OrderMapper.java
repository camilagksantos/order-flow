package com.camilagksantos.orderflow.application.mapper;

import com.camilagksantos.orderflow.application.dto.response.OrderItemResponse;
import com.camilagksantos.orderflow.application.dto.response.OrderResponse;
import com.camilagksantos.orderflow.domain.order.OrderItem;
import com.camilagksantos.orderflow.domain.order.ShopOrder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    OrderResponse toResponse(ShopOrder domain);

    @Mapping(target = "subtotal", expression = "java(domain.subtotal())")
    OrderItemResponse toItemResponse(OrderItem domain);
}