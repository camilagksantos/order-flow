package com.camilagksantos.orderflow.domain.order;

import com.camilagksantos.orderflow.domain.cart.Cart;
import com.camilagksantos.orderflow.domain.shared.Money;
import com.camilagksantos.orderflow.domain.exception.InvalidOrderStatusTransitionException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopOrder {
    private String id;
    private String orderNumber;
    private Long customerId;
    private OrderStatus status;
    private List<OrderItem> items;
    private Money subtotal;
    private Money shippingCost;
    private Money discountAmount;
    private Money totalAmount;
    private PaymentMethod paymentMethod;
    private String trackingCode;
    private String cancelReason;
    private String idempotencyKey;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime paidAt;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime cancelledAt;

    public void pay() {
        validateTransition(OrderStatus.PAID);
        this.status = OrderStatus.PAID;
        this.paidAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void startPreparing() {
        validateTransition(OrderStatus.PREPARING);
        this.status = OrderStatus.PREPARING;
        this.updatedAt = LocalDateTime.now();
    }

    public void ship(String trackingCode) {
        validateTransition(OrderStatus.SHIPPED);
        this.status = OrderStatus.SHIPPED;
        this.trackingCode = trackingCode;
        this.shippedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void deliver() {
        validateTransition(OrderStatus.DELIVERED);
        this.status = OrderStatus.DELIVERED;
        this.deliveredAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void cancel(String reason) {
        validateTransition(OrderStatus.CANCELLED);
        this.status = OrderStatus.CANCELLED;
        this.cancelReason = reason;
        this.cancelledAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    private void validateTransition(OrderStatus target) {
        boolean valid = switch (status) {
            case PENDING -> target == OrderStatus.PAID || target == OrderStatus.CANCELLED;
            case PAID -> target == OrderStatus.PREPARING;
            case PREPARING -> target == OrderStatus.SHIPPED || target == OrderStatus.CANCELLED;
            case SHIPPED -> target == OrderStatus.DELIVERED;
            default -> false;
        };
        if (!valid) throw new InvalidOrderStatusTransitionException(status.name(), target.name());
    }

    public static ShopOrder fromCart(Cart cart, String idempotencyKey) {
        List<OrderItem> orderItems = cart.getItems().stream()
                .map(item -> OrderItem.builder()
                        .id(UUID.randomUUID().toString())
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .productSku(item.getProductSku())
                        .unitPrice(item.getUnitPrice())
                        .quantity(item.getQuantity())
                        .build())
                .collect(Collectors.toList());

        Money total = cart.total();

        return ShopOrder.builder()
                .id(UUID.randomUUID().toString())
                .orderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .customerId(cart.getCustomerId())
                .status(OrderStatus.PENDING)
                .items(orderItems)
                .subtotal(total)
                .shippingCost(Money.zero())
                .discountAmount(Money.zero())
                .totalAmount(total)
                .idempotencyKey(idempotencyKey)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}