package com.camilagksantos.orderflow.domain.order;

import com.camilagksantos.orderflow.domain.shared.Money;

import java.time.LocalDateTime;
import java.util.List;

public record ShopOrder(
        String id,
        String orderNumber,
        Long customerId,
        OrderStatus status,
        List<OrderItem> items,
        Money subtotal,
        Money shippingCost,
        Money discountAmount,
        Money totalAmount,
        PaymentMethod paymentMethod,
        String trackingCode,
        String cancelReason,
        String idempotencyKey,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime paidAt,
        LocalDateTime shippedAt,
        LocalDateTime deliveredAt,
        LocalDateTime cancelledAt
) {
    public ShopOrder {
        if (customerId == null) throw new IllegalArgumentException("Customer ID must not be null");
        if (orderNumber == null || orderNumber.isBlank()) throw new IllegalArgumentException("Order number must not be blank");
        if (status == null) throw new IllegalArgumentException("Status must not be null");
        if (paymentMethod == null) throw new IllegalArgumentException("Payment method must not be null");
        if (idempotencyKey == null || idempotencyKey.isBlank()) throw new IllegalArgumentException("Idempotency key must not be blank");
        items = items == null ? List.of() : List.copyOf(items);
    }

    public ShopOrder pay() {
        validateTransition(OrderStatus.PAID);
        return new ShopOrder(id, orderNumber, customerId, OrderStatus.PAID, items, subtotal, shippingCost, discountAmount, totalAmount, paymentMethod, trackingCode, cancelReason, idempotencyKey, createdAt, LocalDateTime.now(), LocalDateTime.now(), shippedAt, deliveredAt, cancelledAt);
    }

    public ShopOrder startPreparing() {
        validateTransition(OrderStatus.PREPARING);
        return new ShopOrder(id, orderNumber, customerId, OrderStatus.PREPARING, items, subtotal, shippingCost, discountAmount, totalAmount, paymentMethod, trackingCode, cancelReason, idempotencyKey, createdAt, LocalDateTime.now(), paidAt, shippedAt, deliveredAt, cancelledAt);
    }

    public ShopOrder ship(String trackingCode) {
        validateTransition(OrderStatus.SHIPPED);
        return new ShopOrder(id, orderNumber, customerId, OrderStatus.SHIPPED, items, subtotal, shippingCost, discountAmount, totalAmount, paymentMethod, trackingCode, cancelReason, idempotencyKey, createdAt, LocalDateTime.now(), paidAt, LocalDateTime.now(), deliveredAt, cancelledAt);
    }

    public ShopOrder deliver() {
        validateTransition(OrderStatus.DELIVERED);
        return new ShopOrder(id, orderNumber, customerId, OrderStatus.DELIVERED, items, subtotal, shippingCost, discountAmount, totalAmount, paymentMethod, trackingCode, cancelReason, idempotencyKey, createdAt, LocalDateTime.now(), paidAt, shippedAt, LocalDateTime.now(), cancelledAt);
    }

    public ShopOrder cancel(String reason) {
        validateTransition(OrderStatus.CANCELLED);
        return new ShopOrder(id, orderNumber, customerId, OrderStatus.CANCELLED, items, subtotal, shippingCost, discountAmount, totalAmount, paymentMethod, trackingCode, reason, idempotencyKey, createdAt, LocalDateTime.now(), paidAt, shippedAt, deliveredAt, LocalDateTime.now());
    }

    private void validateTransition(OrderStatus target) {
        boolean valid = switch (status) {
            case PENDING -> target == OrderStatus.PAID || target == OrderStatus.CANCELLED;
            case PAID -> target == OrderStatus.PREPARING;
            case PREPARING -> target == OrderStatus.SHIPPED || target == OrderStatus.CANCELLED;
            case SHIPPED -> target == OrderStatus.DELIVERED;
            default -> false;
        };
        if (!valid) throw new IllegalStateException("Cannot transition from " + status + " to " + target);
    }
}
