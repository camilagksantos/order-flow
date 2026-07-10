package com.camilagksantos.orderflow.application.port.input;

import com.camilagksantos.orderflow.domain.order.ShopOrder;

public interface CancelOrderUseCase {
    ShopOrder cancelOrder(String orderId, String reason);
}