package com.camilagksantos.orderflow.application.port.output;

import com.camilagksantos.orderflow.domain.order.ShopOrder;

public interface EmailNotificationPort {

    void sendOrderConfirmation(ShopOrder order);
    void sendOrderShipped(ShopOrder order);
    void sendOrderCancelled(ShopOrder order);
}
