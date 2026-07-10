package com.camilagksantos.orderflow.application.service;

import com.camilagksantos.orderflow.application.port.input.CancelOrderUseCase;
import com.camilagksantos.orderflow.application.port.input.CheckoutUseCase;
import com.camilagksantos.orderflow.application.port.input.FindOrderUseCase;
import com.camilagksantos.orderflow.application.port.input.UpdateOrderStatusUseCase;
import com.camilagksantos.orderflow.application.port.output.CartRepositoryPort;
import com.camilagksantos.orderflow.application.port.output.OrderRepositoryPort;
import com.camilagksantos.orderflow.application.port.output.OutboxEventRepositoryPort;
import com.camilagksantos.orderflow.domain.cart.Cart;
import com.camilagksantos.orderflow.domain.event.OutboxEvent;
import com.camilagksantos.orderflow.domain.event.OutboxEventStatus;
import com.camilagksantos.orderflow.domain.exception.BusinessRuleException;
import com.camilagksantos.orderflow.domain.exception.CartNotFoundException;
import com.camilagksantos.orderflow.domain.exception.OrderNotFoundException;
import com.camilagksantos.orderflow.domain.order.OrderStatus;
import com.camilagksantos.orderflow.domain.order.ShopOrder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService implements CheckoutUseCase, FindOrderUseCase, UpdateOrderStatusUseCase, CancelOrderUseCase {

    private final OrderRepositoryPort orderRepositoryPort;
    private final CartRepositoryPort cartRepositoryPort;
    private final OutboxEventRepositoryPort outboxEventRepositoryPort;

    @Override
    @Transactional
    public ShopOrder checkout(Long customerId, String idempotencyKey) {
        orderRepositoryPort.findByIdempotencyKey(idempotencyKey)
                .ifPresent(order -> {
                    throw new BusinessRuleException("Order already exists for idempotency key: " + idempotencyKey);
                });

        Cart cart = cartRepositoryPort.findActiveByCustomerId(customerId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found for customer: " + customerId));

        if (cart.isEmpty()) throw new BusinessRuleException("Cart is empty");

        ShopOrder order = ShopOrder.fromCart(cart, idempotencyKey);
        ShopOrder savedOrder = orderRepositoryPort.save(order);

        outboxEventRepositoryPort.save(new OutboxEvent(
                UUID.randomUUID().toString(),
                "ORDER_CREATED",
                savedOrder.getId(),
                OutboxEventStatus.PENDING,
                LocalDateTime.now()
        ));

        cart.convert();
        cartRepositoryPort.save(cart);
        return savedOrder;
    }

    @Override
    public ShopOrder findOrderById(String id) {
        return orderRepositoryPort.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Override
    public ShopOrder findOrderByNumber(String orderNumber) {
        return orderRepositoryPort.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException(orderNumber));
    }

    @Override
    public List<ShopOrder> findOrdersByCustomerId(Long customerId) {
        return orderRepositoryPort.findByCustomerId(customerId);
    }

    @Override
    @Transactional
    public ShopOrder updateOrderStatus(String orderId, OrderStatus status) {
        ShopOrder order = findOrderById(orderId);
        switch (status) {
            case PAID -> order.pay();
            case PREPARING -> order.startPreparing();
            case SHIPPED -> order.ship(order.getTrackingCode());
            case DELIVERED -> order.deliver();
            default -> throw new BusinessRuleException("Invalid status transition to: " + status);
        }
        return orderRepositoryPort.save(order);
    }

    @Override
    @Transactional
    public ShopOrder cancelOrder(String orderId, String reason) {
        ShopOrder order = findOrderById(orderId);
        order.cancel(reason);
        return orderRepositoryPort.save(order);
    }
}