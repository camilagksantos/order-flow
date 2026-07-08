package com.camilagksantos.orderflow.application.port.input;

import com.camilagksantos.orderflow.domain.order.ShopOrder;

import java.util.List;

public interface FindOrderUseCase {

    ShopOrder findById(String id);
    ShopOrder findByOrderNumber(String orderNumber);
    List<ShopOrder> findByCustomerId(Long customerId);
}
