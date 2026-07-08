package com.camilagksantos.orderflow.application.port.input;

import com.camilagksantos.orderflow.domain.order.OrderStatus;
import com.camilagksantos.orderflow.domain.order.ShopOrder;

public interface UpdateOrderStatusUseCase {

    ShopOrder execute(String orderId, OrderStatus status);
}
