package com.camilagksantos.orderflow.domain.order;

import com.camilagksantos.orderflow.domain.shared.Money;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    private String id;
    private String orderId;
    private Long productId;
    private String productName;
    private String productSku;
    private Money unitPrice;
    private int quantity;

    public Money subtotal() {
        return unitPrice.multiply(quantity);
    }
}