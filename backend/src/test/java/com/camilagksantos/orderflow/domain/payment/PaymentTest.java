package com.camilagksantos.orderflow.domain.payment;

import com.camilagksantos.orderflow.domain.order.PaymentMethod;
import com.camilagksantos.orderflow.domain.shared.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class PaymentTest {

    private Payment payment;

    @BeforeEach
    void setUp() {
        payment = Payment.builder()
                .id(UUID.randomUUID().toString())
                .orderId(UUID.randomUUID().toString())
                .amount(Money.of(BigDecimal.valueOf(100)))
                .method(PaymentMethod.MBWAY)
                .status(PaymentStatus.PENDING)
                .attemptCount(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void shouldApprovePayment() {
        payment.approve("TX-123");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(payment.getTransactionId()).isEqualTo("TX-123");
        assertThat(payment.getProcessedAt()).isNotNull();
    }

    @Test
    void shouldDeclinePayment() {
        payment.decline();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.DECLINED);
        assertThat(payment.getAttemptCount()).isEqualTo(1);
    }

    @Test
    void shouldIncrementAttemptCountOnDecline() {
        payment.decline();
        payment.decline();
        assertThat(payment.getAttemptCount()).isEqualTo(2);
    }
}