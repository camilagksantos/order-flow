package com.camilagksantos.orderflow.application.service;

import com.camilagksantos.orderflow.application.port.output.CartRepositoryPort;
import com.camilagksantos.orderflow.application.port.output.CustomerRepositoryPort;
import com.camilagksantos.orderflow.application.port.output.OrderRepositoryPort;
import com.camilagksantos.orderflow.application.port.output.OutboxEventRepositoryPort;
import com.camilagksantos.orderflow.domain.cart.Cart;
import com.camilagksantos.orderflow.domain.cart.CartItem;
import com.camilagksantos.orderflow.domain.cart.CartStatus;
import com.camilagksantos.orderflow.domain.customer.Customer;
import com.camilagksantos.orderflow.domain.customer.CustomerStatus;
import com.camilagksantos.orderflow.domain.exception.BusinessRuleException;
import com.camilagksantos.orderflow.domain.exception.CartNotFoundException;
import com.camilagksantos.orderflow.domain.exception.OrderNotFoundException;
import com.camilagksantos.orderflow.domain.order.OrderStatus;
import com.camilagksantos.orderflow.domain.order.PaymentMethod;
import com.camilagksantos.orderflow.domain.order.ShopOrder;
import com.camilagksantos.orderflow.domain.shared.Email;
import com.camilagksantos.orderflow.domain.shared.Money;
import com.camilagksantos.orderflow.domain.shared.NIF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepositoryPort orderRepositoryPort;

    @Mock
    private CartRepositoryPort cartRepositoryPort;

    @Mock
    private OutboxEventRepositoryPort outboxEventRepositoryPort;

    @Mock
    private CustomerRepositoryPort customerRepositoryPort;

    @InjectMocks
    private OrderService orderService;

    private Cart cart;
    private Customer customer;
    private ShopOrder order;

    @BeforeEach
    void setUp() {
        CartItem item = CartItem.builder()
                .id(UUID.randomUUID().toString())
                .productId(1L)
                .productName("Product A")
                .productSku("SKU-001")
                .unitPrice(Money.of(BigDecimal.valueOf(10)))
                .quantity(2)
                .build();

        cart = Cart.builder()
                .id(UUID.randomUUID().toString())
                .customerId(1L)
                .status(CartStatus.ACTIVE)
                .items(new ArrayList<>(List.of(item)))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        customer = Customer.builder()
                .id(1L)
                .userId(1L)
                .name("Camila Kfouri")
                .email(new Email("camila@test.com"))
                .nif(new NIF("123456789"))
                .status(CustomerStatus.ACTIVE)
                .addresses(List.of())
                .build();

        order = ShopOrder.builder()
                .id(UUID.randomUUID().toString())
                .orderNumber("ORD-TEST-001")
                .customerId(1L)
                .customerEmail("camila@test.com")
                .status(OrderStatus.PENDING)
                .items(List.of())
                .subtotal(Money.of(BigDecimal.valueOf(20)))
                .shippingCost(Money.zero())
                .discountAmount(Money.zero())
                .totalAmount(Money.of(BigDecimal.valueOf(20)))
                .paymentMethod(PaymentMethod.MBWAY)
                .idempotencyKey(UUID.randomUUID().toString())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void shouldCheckoutSuccessfully() {
        String idempotencyKey = UUID.randomUUID().toString();
        when(orderRepositoryPort.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(cartRepositoryPort.findActiveByCustomerId(1L)).thenReturn(Optional.of(cart));
        when(customerRepositoryPort.findById(1L)).thenReturn(Optional.of(customer));
        when(orderRepositoryPort.save(any())).thenReturn(order);
        when(outboxEventRepositoryPort.save(any())).thenReturn(null);
        when(cartRepositoryPort.save(any())).thenReturn(cart);

        ShopOrder result = orderService.checkout(1L, idempotencyKey);

        assertThat(result).isNotNull();
        assertThat(result.getOrderNumber()).isEqualTo("ORD-TEST-001");
        verify(orderRepositoryPort).save(any());
        verify(outboxEventRepositoryPort).save(any());
    }

    @Test
    void shouldThrowWhenCartIsEmpty() {
        String idempotencyKey = UUID.randomUUID().toString();
        Cart emptyCart = Cart.builder()
                .id(UUID.randomUUID().toString())
                .customerId(1L)
                .status(CartStatus.ACTIVE)
                .items(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(orderRepositoryPort.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(cartRepositoryPort.findActiveByCustomerId(1L)).thenReturn(Optional.of(emptyCart));

        assertThatThrownBy(() -> orderService.checkout(1L, idempotencyKey))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void shouldThrowWhenCartNotFound() {
        String idempotencyKey = UUID.randomUUID().toString();
        when(orderRepositoryPort.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(cartRepositoryPort.findActiveByCustomerId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.checkout(1L, idempotencyKey))
                .isInstanceOf(CartNotFoundException.class);
    }

    @Test
    void shouldThrowWhenDuplicateIdempotencyKey() {
        String idempotencyKey = UUID.randomUUID().toString();
        when(orderRepositoryPort.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.checkout(1L, idempotencyKey))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void shouldFindOrderById() {
        when(orderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));
        ShopOrder found = orderService.findOrderById(order.getId());
        assertThat(found.getOrderNumber()).isEqualTo("ORD-TEST-001");
    }

    @Test
    void shouldThrowWhenOrderNotFound() {
        when(orderRepositoryPort.findById("invalid-id")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> orderService.findOrderById("invalid-id"))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void shouldCancelOrder() {
        when(orderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepositoryPort.save(any())).thenReturn(order);

        ShopOrder cancelled = orderService.cancelOrder(order.getId(), "Customer request");

        verify(orderRepositoryPort).save(any());
    }

    @Test
    void shouldUpdateOrderStatusToPaid() {
        when(orderRepositoryPort.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepositoryPort.save(any())).thenReturn(order);

        orderService.updateOrderStatus(order.getId(), OrderStatus.PAID);

        verify(orderRepositoryPort).save(any());
    }
}