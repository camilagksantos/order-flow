package com.camilagksantos.orderflow.application.mapper;

import com.camilagksantos.orderflow.application.dto.response.CartItemResponse;
import com.camilagksantos.orderflow.application.dto.response.CartResponse;
import com.camilagksantos.orderflow.domain.cart.Cart;
import com.camilagksantos.orderflow.domain.cart.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CartMapper {

    @Mapping(target = "total", expression = "java(domain.total())")
    CartResponse toResponse(Cart domain);

    @Mapping(target = "subtotal", expression = "java(domain.subtotal())")
    CartItemResponse toItemResponse(CartItem domain);
}