package com.camilagksantos.orderflow.infrastructure.adapter.output.persistence;

import com.camilagksantos.orderflow.BaseIntegrationTest;
import com.camilagksantos.orderflow.domain.cart.Cart;
import com.camilagksantos.orderflow.domain.cart.CartItem;
import com.camilagksantos.orderflow.domain.customer.Customer;
import com.camilagksantos.orderflow.domain.customer.CustomerStatus;
import com.camilagksantos.orderflow.domain.order.PaymentMethod;
import com.camilagksantos.orderflow.domain.order.ShopOrder;
import com.camilagksantos.orderflow.domain.payment.Payment;
import com.camilagksantos.orderflow.domain.payment.PaymentStatus;
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
class PaymentFlowIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private PaymentJpaAdapter paymentJpaAdapter;

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

    private ShopOrder persistTestOrder(Customer customer) {
        Cart cart = Cart.newCart(customer.getId());
        cart.addItem(CartItem.builder()
                .id(UUID.randomUUID().toString())
                .productId(1L)
                .productName("Laptop")
                .productSku("LAP-001")
                .unitPrice(Money.of(BigDecimal.valueOf(999)))
                .quantity(2)
                .build());

        ShopOrder order = ShopOrder.fromCart(cart, UUID.randomUUID().toString(), customer.getEmail().value(), PaymentMethod.MBWAY);
        return orderJpaAdapter.save(order);
    }

    private Payment buildPayment(String orderId, Money amount) {
        return Payment.builder()
                .id(UUID.randomUUID().toString())
                .orderId(orderId)
                .amount(amount)
                .method(PaymentMethod.MBWAY)
                .status(PaymentStatus.PENDING)
                .attemptCount(0)
                .build();
    }

    @Test
    void shouldCreatePayment() {
        Customer customer = persistTestCustomer();
        ShopOrder order = persistTestOrder(customer);

        Payment payment = buildPayment(order.getId(), order.getTotalAmount());

        Payment saved = paymentJpaAdapter.save(payment);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getOrderId()).isEqualTo(order.getId());
        assertThat(saved.getAmount().amount()).isEqualByComparingTo(order.getTotalAmount().amount());
        assertThat(saved.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void shouldFindPaymentByOrderId() {
        Customer customer = persistTestCustomer();
        ShopOrder order = persistTestOrder(customer);

        paymentJpaAdapter.save(buildPayment(order.getId(), order.getTotalAmount()));

        Optional<Payment> found = paymentJpaAdapter.findByOrderId(order.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getOrderId()).isEqualTo(order.getId());
    }

    @Test
    void shouldApprovePayment() {
        Customer customer = persistTestCustomer();
        ShopOrder order = persistTestOrder(customer);

        Payment payment = paymentJpaAdapter.save(buildPayment(order.getId(), order.getTotalAmount()));

        payment.approve("TXN-123456");
        Payment approved = paymentJpaAdapter.save(payment);

        assertThat(approved.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(approved.getTransactionId()).isEqualTo("TXN-123456");
        assertThat(approved.getProcessedAt()).isNotNull();
    }

    @Test
    void shouldDeclinePaymentAndIncrementAttemptCount() {
        Customer customer = persistTestCustomer();
        ShopOrder order = persistTestOrder(customer);

        Payment payment = paymentJpaAdapter.save(buildPayment(order.getId(), order.getTotalAmount()));
        assertThat(payment.getAttemptCount()).isEqualTo(0);

        payment.decline();
        Payment declined = paymentJpaAdapter.save(payment);

        assertThat(declined.getStatus()).isEqualTo(PaymentStatus.DECLINED);
        assertThat(declined.getAttemptCount()).isEqualTo(1);
    }
}