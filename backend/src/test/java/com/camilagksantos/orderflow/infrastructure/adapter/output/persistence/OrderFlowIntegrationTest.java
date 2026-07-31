package com.camilagksantos.orderflow.infrastructure.adapter.output.persistence;

import com.camilagksantos.orderflow.BaseIntegrationTest;
import com.camilagksantos.orderflow.domain.cart.Cart;
import com.camilagksantos.orderflow.domain.cart.CartItem;
import com.camilagksantos.orderflow.domain.customer.Customer;
import com.camilagksantos.orderflow.domain.customer.CustomerStatus;
import com.camilagksantos.orderflow.domain.order.OrderStatus;
import com.camilagksantos.orderflow.domain.order.PaymentMethod;
import com.camilagksantos.orderflow.domain.order.ShopOrder;
import com.camilagksantos.orderflow.domain.shared.Email;
import com.camilagksantos.orderflow.domain.shared.Money;
import com.camilagksantos.orderflow.domain.shared.NIF;
import com.camilagksantos.orderflow.infrastructure.persistence.entity.UserEntity;
import com.camilagksantos.orderflow.infrastructure.persistence.repository.UserJpaRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@Transactional
class OrderFlowIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private OrderJpaAdapter orderJpaAdapter;

    @Autowired
    private CustomerJpaAdapter customerJpaAdapter;

    @Autowired
    private UserJpaRepository userJpaRepository;

    private Customer persistTestCustomer() {
        UserEntity user = new UserEntity();
        user.setEmail("user-" + UUID.randomUUID() + "@example.com");
        user.setPassword("encoded-password");
        user.setActive(true);
        user.setRoles(List.of());
        Long userId = userJpaRepository.save(user).getId();

        Customer customer = Customer.builder()
                .userId(userId)
                .name("Test Customer")
                .email(new Email("customer-" + UUID.randomUUID() + "@example.com"))
                .nif(new NIF("123456789"))
                .phone("912345678")
                .status(CustomerStatus.ACTIVE)
                .addresses(new ArrayList<>())
                .build();

        return customerJpaAdapter.save(customer);
    }

    private Cart buildCartWithItems(Long customerId) {
        Cart cart = Cart.newCart(customerId);
        cart.addItem(CartItem.builder()
                .id(UUID.randomUUID().toString())
                .productId(1L)
                .productName("Laptop")
                .productSku("LAP-001")
                .unitPrice(Money.of(BigDecimal.valueOf(999)))
                .quantity(2)
                .build());
        return cart;
    }

    private ShopOrder buildOrderFromCart(Customer customer) {
        Cart cart = buildCartWithItems(customer.getId());
        return ShopOrder.fromCart(cart, UUID.randomUUID().toString(), customer.getEmail().value(), PaymentMethod.CREDIT_CARD);
    }

    @Test
    void shouldCreateOrder() {
        Customer customer = persistTestCustomer();
        ShopOrder order = buildOrderFromCart(customer);

        ShopOrder saved = orderJpaAdapter.save(order);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getOrderNumber()).isNotNull();
        assertThat(saved.getCustomerId()).isEqualTo(customer.getId());
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(saved.getTotalAmount().amount()).isEqualByComparingTo(BigDecimal.valueOf(1998));
    }

    @Test
    void shouldCreateOrderWithItems() {
        Customer customer = persistTestCustomer();
        ShopOrder order = buildOrderFromCart(customer);

        ShopOrder saved = orderJpaAdapter.save(order);

        assertThat(saved.getItems()).hasSize(1);
        assertThat(saved.getItems().get(0).getProductName()).isEqualTo("Laptop");
        assertThat(saved.getItems().get(0).getSubtotal().amount()).isEqualByComparingTo(BigDecimal.valueOf(1998));
    }

    @Test
    void shouldFindOrderById() {
        Customer customer = persistTestCustomer();
        ShopOrder order = orderJpaAdapter.save(buildOrderFromCart(customer));

        Optional<ShopOrder> found = orderJpaAdapter.findById(order.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getOrderNumber()).isEqualTo(order.getOrderNumber());
    }

    @Test
    void shouldFindOrderByOrderNumber() {
        Customer customer = persistTestCustomer();
        ShopOrder order = orderJpaAdapter.save(buildOrderFromCart(customer));

        Optional<ShopOrder> found = orderJpaAdapter.findByOrderNumber(order.getOrderNumber());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(order.getId());
    }

    @Test
    void shouldFindOrderByIdempotencyKey() {
        Customer customer = persistTestCustomer();
        ShopOrder order = orderJpaAdapter.save(buildOrderFromCart(customer));

        Optional<ShopOrder> found = orderJpaAdapter.findByIdempotencyKey(order.getIdempotencyKey());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(order.getId());
    }

    @Test
    void shouldFindOrdersByCustomerId() {
        Customer customer = persistTestCustomer();
        orderJpaAdapter.save(buildOrderFromCart(customer));
        orderJpaAdapter.save(buildOrderFromCart(customer));

        List<ShopOrder> orders = orderJpaAdapter.findByCustomerId(customer.getId());

        assertThat(orders).hasSize(2);
    }

    @Test
    void shouldTransitionOrderThroughFullLifecycle() {
        Customer customer = persistTestCustomer();
        ShopOrder order = orderJpaAdapter.save(buildOrderFromCart(customer));

        order.pay();
        order = orderJpaAdapter.save(order);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getPaidAt()).isNotNull();

        order.startPreparing();
        order = orderJpaAdapter.save(order);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PREPARING);

        order.ship("TRACK-123456");
        order = orderJpaAdapter.save(order);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(order.getTrackingCode()).isEqualTo("TRACK-123456");

        order.deliver();
        order = orderJpaAdapter.save(order);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(order.getDeliveredAt()).isNotNull();
    }

    @Test
    void shouldCancelOrder() {
        Customer customer = persistTestCustomer();
        ShopOrder order = orderJpaAdapter.save(buildOrderFromCart(customer));

        order.cancel("Customer changed their mind");
        ShopOrder cancelled = orderJpaAdapter.save(order);

        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelled.getCancelReason()).isEqualTo("Customer changed their mind");
        assertThat(cancelled.getCancelledAt()).isNotNull();
    }
}