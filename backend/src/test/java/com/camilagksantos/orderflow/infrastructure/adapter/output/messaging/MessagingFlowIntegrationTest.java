package com.camilagksantos.orderflow.infrastructure.adapter.output.messaging;

import com.camilagksantos.orderflow.BaseIntegrationTest;
import com.camilagksantos.orderflow.application.port.output.OutboxEventRepositoryPort;
import com.camilagksantos.orderflow.application.port.output.ProcessedEventRepositoryPort;
import com.camilagksantos.orderflow.domain.cart.Cart;
import com.camilagksantos.orderflow.domain.cart.CartItem;
import com.camilagksantos.orderflow.domain.category.Category;
import com.camilagksantos.orderflow.domain.customer.Customer;
import com.camilagksantos.orderflow.domain.customer.CustomerStatus;
import com.camilagksantos.orderflow.domain.event.OutboxEvent;
import com.camilagksantos.orderflow.domain.event.OutboxEventStatus;
import com.camilagksantos.orderflow.domain.order.PaymentMethod;
import com.camilagksantos.orderflow.domain.order.ShopOrder;
import com.camilagksantos.orderflow.domain.product.Product;
import com.camilagksantos.orderflow.domain.product.ProductStatus;
import com.camilagksantos.orderflow.domain.shared.Email;
import com.camilagksantos.orderflow.domain.shared.Money;
import com.camilagksantos.orderflow.domain.shared.NIF;
import com.camilagksantos.orderflow.infrastructure.adapter.output.persistence.CategoryJpaAdapter;
import com.camilagksantos.orderflow.infrastructure.adapter.output.persistence.CustomerJpaAdapter;
import com.camilagksantos.orderflow.infrastructure.adapter.output.persistence.OrderJpaAdapter;
import com.camilagksantos.orderflow.infrastructure.adapter.output.persistence.ProductJpaAdapter;
import com.camilagksantos.orderflow.infrastructure.config.RabbitMQConfig;
import com.camilagksantos.orderflow.infrastructure.persistence.entity.UserEntity;
import com.camilagksantos.orderflow.infrastructure.persistence.repository.CategoryJpaRepository;
import com.camilagksantos.orderflow.infrastructure.persistence.repository.CustomerJpaRepository;
import com.camilagksantos.orderflow.infrastructure.persistence.repository.OutboxEventJpaRepository;
import com.camilagksantos.orderflow.infrastructure.persistence.repository.ProcessedEventJpaRepository;
import com.camilagksantos.orderflow.infrastructure.persistence.repository.ProductJpaRepository;
import com.camilagksantos.orderflow.infrastructure.persistence.repository.ShopOrderJpaRepository;
import com.camilagksantos.orderflow.infrastructure.persistence.repository.UserJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class MessagingFlowIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private OutboxEventScheduler outboxEventScheduler;

    @Autowired
    private OutboxEventRepositoryPort outboxEventRepositoryPort;

    @Autowired
    private ProcessedEventRepositoryPort processedEventRepositoryPort;

    @Autowired
    private OrderJpaAdapter orderJpaAdapter;

    @Autowired
    private ProductJpaAdapter productJpaAdapter;

    @Autowired
    private CategoryJpaAdapter categoryJpaAdapter;

    @Autowired
    private CustomerJpaAdapter customerJpaAdapter;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private ShopOrderJpaRepository shopOrderJpaRepository;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private CategoryJpaRepository categoryJpaRepository;

    @Autowired
    private CustomerJpaRepository customerJpaRepository;

    @Autowired
    private OutboxEventJpaRepository outboxEventJpaRepository;

    @Autowired
    private ProcessedEventJpaRepository processedEventJpaRepository;

    @Autowired
    private org.springframework.amqp.rabbit.core.RabbitAdmin rabbitAdmin;

    private String createdOrderId;
    private Long createdProductId;
    private Long createdCategoryId;
    private Long createdCustomerId;
    private Long createdUserId;
    private String createdOutboxEventId;

    @BeforeEach
    void purgeQueues() {
        rabbitAdmin.purgeQueue(RabbitMQConfig.ORDER_CREATED_QUEUE, false);
        rabbitAdmin.purgeQueue(RabbitMQConfig.ORDER_PAID_QUEUE, false);
        rabbitAdmin.purgeQueue(RabbitMQConfig.ORDER_CANCELLED_QUEUE, false);
        rabbitAdmin.purgeQueue(RabbitMQConfig.ORDER_SHIPPED_QUEUE, false);
    }

    @AfterEach
    void cleanUp() {
        if (createdOrderId != null) shopOrderJpaRepository.deleteById(createdOrderId);
        if (createdOutboxEventId != null) {
            outboxEventJpaRepository.deleteById(createdOutboxEventId);
            processedEventJpaRepository.deleteById(createdOutboxEventId);
        }
        if (createdProductId != null) productJpaRepository.deleteById(createdProductId);
        if (createdCategoryId != null) categoryJpaRepository.deleteById(createdCategoryId);
        if (createdCustomerId != null) customerJpaRepository.deleteById(createdCustomerId);
        if (createdUserId != null) userJpaRepository.deleteById(createdUserId);
    }

    private Customer persistTestCustomer() {
        UserEntity user = new UserEntity();
        user.setEmail("user-" + UUID.randomUUID() + "@example.com");
        user.setPassword("encoded-password");
        user.setActive(true);
        user.setRoles(List.of());
        createdUserId = userJpaRepository.save(user).getId();

        Customer customer = Customer.builder()
                .userId(createdUserId)
                .name("Test Customer")
                .email(new Email("customer-" + UUID.randomUUID() + "@example.com"))
                .nif(new NIF("123456789"))
                .phone("912345678")
                .status(CustomerStatus.ACTIVE)
                .addresses(new ArrayList<>())
                .build();

        Customer saved = customerJpaAdapter.save(customer);
        createdCustomerId = saved.getId();
        return saved;
    }

    private Product persistTestProduct(int reservedQuantity) {
        Category category = categoryJpaAdapter.save(Category.builder().name("Electronics").build());
        createdCategoryId = category.getId();

        Product product = Product.builder()
                .name("Laptop")
                .sku("LAP-" + UUID.randomUUID())
                .price(Money.of(BigDecimal.valueOf(999)))
                .stockQuantity(10)
                .reservedQuantity(reservedQuantity)
                .category(category)
                .status(ProductStatus.ACTIVE)
                .build();

        Product saved = productJpaAdapter.save(product);
        createdProductId = saved.getId();
        return saved;
    }

    private ShopOrder persistTestOrder(Customer customer, Product product, int quantity) {
        Cart cart = Cart.newCart(customer.getId());
        cart.addItem(CartItem.builder()
                .id(UUID.randomUUID().toString())
                .productId(product.getId())
                .productName(product.getName())
                .productSku(product.getSku())
                .unitPrice(product.getPrice())
                .quantity(quantity)
                .build());

        ShopOrder order = ShopOrder.fromCart(cart, UUID.randomUUID().toString(),
                customer.getEmail().value(), PaymentMethod.CREDIT_CARD);

        ShopOrder saved = orderJpaAdapter.save(order);
        createdOrderId = saved.getId();
        return saved;
    }

    private OutboxEvent persistPendingEvent(String eventType, String orderId) {
        OutboxEvent event = outboxEventRepositoryPort.save(new OutboxEvent(
                UUID.randomUUID().toString(),
                eventType,
                orderId,
                OutboxEventStatus.PENDING,
                LocalDateTime.now()
        ));
        createdOutboxEventId = event.id();
        return event;
    }

    @Test
    void shouldReserveStockWhenOrderCreatedEventProcessed() {
        Customer customer = persistTestCustomer();
        Product product = persistTestProduct(0);
        ShopOrder order = persistTestOrder(customer, product, 3);
        assertThat(orderJpaAdapter.findById(order.getId())).isPresent();
        persistPendingEvent("ORDER_CREATED", order.getId());

        outboxEventScheduler.processOutboxEvents();

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            var updated = productJpaRepository.findById(product.getId()).orElseThrow();
            assertThat(updated.getReservedQuantity()).isEqualTo(3);
        });
    }

    @Test
    void shouldConfirmSaleWhenOrderPaidEventProcessed() {
        Customer customer = persistTestCustomer();
        Product product = persistTestProduct(3);
        ShopOrder order = persistTestOrder(customer, product, 3);
        persistPendingEvent("ORDER_PAID", order.getId());

        outboxEventScheduler.processOutboxEvents();

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            var updated = productJpaRepository.findById(product.getId()).orElseThrow();
            assertThat(updated.getStockQuantity()).isEqualTo(7);
            assertThat(updated.getReservedQuantity()).isEqualTo(0);
        });
    }

    @Test
    void shouldReleaseStockWhenOrderCancelledEventProcessed() {
        Customer customer = persistTestCustomer();
        Product product = persistTestProduct(3);
        ShopOrder order = persistTestOrder(customer, product, 3);
        persistPendingEvent("ORDER_CANCELLED", order.getId());

        outboxEventScheduler.processOutboxEvents();

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            var updated = productJpaRepository.findById(product.getId()).orElseThrow();
            assertThat(updated.getReservedQuantity()).isEqualTo(0);
        });
    }

    @Test
    void shouldMarkEventAsProcessedWhenOrderShippedEventHandled() {
        Customer customer = persistTestCustomer();
        Product product = persistTestProduct(0);
        ShopOrder order = persistTestOrder(customer, product, 1);
        OutboxEvent event = persistPendingEvent("ORDER_SHIPPED", order.getId());

        outboxEventScheduler.processOutboxEvents();

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                assertThat(processedEventRepositoryPort.existsById(event.id())).isTrue()
        );
    }

    @Test
    void shouldIgnoreDuplicateEvent() {
        Customer customer = persistTestCustomer();
        Product product = persistTestProduct(0);
        ShopOrder order = persistTestOrder(customer, product, 2);
        OutboxEvent event = persistPendingEvent("ORDER_CREATED", order.getId());

        processedEventRepositoryPort.save(event.id());

        outboxEventScheduler.processOutboxEvents();

        await().atMost(3, TimeUnit.SECONDS).pollDelay(1, TimeUnit.SECONDS).untilAsserted(() -> {
            var unchanged = productJpaRepository.findById(product.getId()).orElseThrow();
            assertThat(unchanged.getReservedQuantity()).isEqualTo(0);
        });
    }

    @Test
    void shouldMarkOutboxEventAsSentAfterPublishing() {
        Customer customer = persistTestCustomer();
        Product product = persistTestProduct(0);
        ShopOrder order = persistTestOrder(customer, product, 1);
        OutboxEvent event = persistPendingEvent("ORDER_CREATED", order.getId());

        outboxEventScheduler.processOutboxEvents();

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<OutboxEvent> pending = outboxEventRepositoryPort.findByStatus(OutboxEventStatus.PENDING);
            assertThat(pending).noneMatch(e -> e.id().equals(event.id()));
        });
    }
}