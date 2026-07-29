package com.camilagksantos.orderflow.application.service;

import com.camilagksantos.orderflow.application.port.output.PaymentRepositoryPort;
import com.camilagksantos.orderflow.domain.order.PaymentMethod;
import com.camilagksantos.orderflow.domain.payment.Payment;
import com.camilagksantos.orderflow.domain.payment.PaymentStatus;
import com.camilagksantos.orderflow.domain.shared.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepositoryPort paymentRepositoryPort;

    @InjectMocks
    private PaymentService paymentService;

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
    void shouldProcessPayment() {
        when(paymentRepositoryPort.save(payment)).thenReturn(payment);
        Payment processed = paymentService.processPayment(payment);
        assertThat(processed).isNotNull();
        verify(paymentRepositoryPort).save(payment);
    }
}