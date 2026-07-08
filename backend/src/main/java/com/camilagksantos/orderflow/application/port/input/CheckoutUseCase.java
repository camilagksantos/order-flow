package com.camilagksantos.orderflow.application.port.input;

import com.camilagksantos.orderflow.domain.order.ShopOrder;

public interface CheckoutUseCase {

    ShopOrder execute(Long customerId, String idempotencyKey);
}
