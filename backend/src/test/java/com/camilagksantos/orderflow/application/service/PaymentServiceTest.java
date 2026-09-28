package com.camilagksantos.orderflow.application.service;

import com.camilagksantos.orderflow.application.port.output.OrderRepositoryPort;
import com.camilagksantos.orderflow.application.port.output.OutboxEventRepositoryPort;
import com.camilagksantos.orderflow.application.port.output.PaymentRepositoryPort;
import com.camilagksantos.orderflow.domain.exception.BusinessRuleException;
import com.camilagksantos.orderflow.domain.exception.OrderNotFoundException;
import com.camilagksantos.orderflow.domain.order.OrderStatus;
import com.camilagksantos.orderflow.domain.order.PaymentMethod;
import com.camilagksantos.orderflow.domain.order.ShopOrder;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepositoryPort paymentRepositoryPort;

    @Mock
    private OrderRepositoryPort orderRepositoryPort;

    @Mock
    private OutboxEventRepositoryPort outboxEventRepositoryPort;

    @InjectMocks
    private PaymentService paymentService;

    private ShopOrder order;
    private Payment payment;

    @BeforeEach
    void setUp() {
        order = ShopOrder.builder()
                .id(UUID.randomUUID().toString())
                .orderNumber("ORD-TEST-001")
                .customerId(1L)
                .status(OrderStatus.PENDING)
                .totalAmount(Money.of(BigDecimal.valueOf(100)))
                .paymentMethod(PaymentMethod.MBWAY)
                .build();

        payment = Payment.builder()
                .orderId(order.getId())
                .method(PaymentMethod.MBWAY)
                .mbwayPhone("912345678")
                .build();
    }

    @Test
    void shouldProcessPaymentAndMarkOrderPaid() {
        when(orderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));
        when(paymentRepositoryPort.findByOrderId(order.getId())).thenReturn(Optional.empty());
        when(paymentRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Payment processed = paymentService.processPayment(payment);

        assertThat(processed.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(processed.getAmount()).isEqualTo(order.getTotalAmount());
        assertThat(processed.getTransactionId()).isNotBlank();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(orderRepositoryPort).save(order);
        verify(outboxEventRepositoryPort).save(any());
    }

    @Test
    void shouldThrowWhenOrderNotFound() {
        payment.setOrderId("missing");
        when(orderRepositoryPort.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.processPayment(payment))
                .isInstanceOf(OrderNotFoundException.class);

        verify(paymentRepositoryPort, never()).save(any());
    }

    @Test
    void shouldThrowWhenOrderIsNotPending() {
        order.pay();
        when(orderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.processPayment(payment))
                .isInstanceOf(BusinessRuleException.class);

        verify(paymentRepositoryPort, never()).save(any());
    }

    @Test
    void shouldThrowWhenPaymentMethodDiffersFromOrder() {
        payment.setMethod(PaymentMethod.CREDIT_CARD);
        when(orderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.processPayment(payment))
                .isInstanceOf(BusinessRuleException.class);

        verify(paymentRepositoryPort, never()).save(any());
    }

    @Test
    void shouldThrowWhenPaymentAlreadyExists() {
        when(orderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));
        when(paymentRepositoryPort.findByOrderId(order.getId())).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.processPayment(payment))
                .isInstanceOf(BusinessRuleException.class);

        verify(paymentRepositoryPort, never()).save(any());
    }
}