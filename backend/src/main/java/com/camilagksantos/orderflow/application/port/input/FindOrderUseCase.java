package com.camilagksantos.orderflow.application.port.input;

import com.camilagksantos.orderflow.domain.order.ShopOrder;

import java.util.List;

public interface FindOrderUseCase {

    ShopOrder findOrderById(String id);
    ShopOrder findOrderByNumber(String orderNumber);
    List<ShopOrder> findOrderByCustomerId(Long customerId);
}
