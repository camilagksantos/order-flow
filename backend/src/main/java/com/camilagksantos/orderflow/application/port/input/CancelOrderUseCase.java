package com.camilagksantos.orderflow.application.port.input;

import com.camilagksantos.orderflow.domain.order.ShopOrder;

public interface CancelOrderUseCase {
    ShopOrder execute(String orderId, String reason);
}