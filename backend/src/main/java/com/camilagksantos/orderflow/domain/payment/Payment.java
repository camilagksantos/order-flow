package com.camilagksantos.orderflow.domain.payment;

import com.camilagksantos.orderflow.domain.order.PaymentMethod;
import com.camilagksantos.orderflow.domain.shared.Money;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    private String id;
    private String orderId;
    private Money amount;
    private PaymentMethod method;
    private PaymentStatus status;
    private String transactionId;
    private String cardLastFour;
    private String cardBrand;
    private String mbwayPhone;
    private String mbEntity;
    private String mbReference;
    private int attemptCount;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;

    public void approve(String transactionId) {
        this.status = PaymentStatus.APPROVED;
        this.transactionId = transactionId;
        this.processedAt = LocalDateTime.now();
    }

    public void decline() {
        this.status = PaymentStatus.DECLINED;
        this.attemptCount++;
    }
}