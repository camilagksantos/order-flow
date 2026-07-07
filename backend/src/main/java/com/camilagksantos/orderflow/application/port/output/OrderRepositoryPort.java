package com.camilagksantos.orderflow.application.port.output;

import com.camilagksantos.orderflow.domain.order.ShopOrder;

import java.util.List;
import java.util.Optional;

public interface OrderRepositoryPort {

    ShopOrder save(ShopOrder order);
    Optional<ShopOrder> findById(String id);
    Optional<ShopOrder> findByOrderNumber(String orderNumber);
    Optional<ShopOrder> findByIdempotencyKey(String idempotencyKey);
    List<ShopOrder> findByCustomerId(Long customerId);
}
