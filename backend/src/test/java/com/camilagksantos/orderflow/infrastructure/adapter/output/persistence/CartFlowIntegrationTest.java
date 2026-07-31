package com.camilagksantos.orderflow.infrastructure.adapter.output.persistence;

import com.camilagksantos.orderflow.BaseIntegrationTest;
import com.camilagksantos.orderflow.domain.cart.Cart;
import com.camilagksantos.orderflow.domain.cart.CartItem;
import com.camilagksantos.orderflow.domain.cart.CartStatus;
import com.camilagksantos.orderflow.domain.customer.Customer;
import com.camilagksantos.orderflow.domain.customer.CustomerStatus;
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
class CartFlowIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private CartJpaAdapter cartJpaAdapter;

    @Autowired
    private CustomerJpaAdapter customerJpaAdapter;

    @Autowired
    private UserJpaRepository userJpaRepository;

    private Long persistTestCustomer() {
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

        return customerJpaAdapter.save(customer).getId();
    }

    private CartItem buildCartItem() {
        return CartItem.builder()
                .id(UUID.randomUUID().toString())
                .productId(1L)
                .productName("Laptop")
                .productSku("LAP-001")
                .unitPrice(Money.of(BigDecimal.valueOf(999)))
                .quantity(2)
                .build();
    }

    @Test
    void shouldCreateCart() {
        Long customerId = persistTestCustomer();

        Cart cart = Cart.newCart(customerId);

        Cart saved = cartJpaAdapter.save(cart);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCustomerId()).isEqualTo(customerId);
        assertThat(saved.getStatus()).isEqualTo(CartStatus.ACTIVE);
        assertThat(saved.isEmpty()).isTrue();
    }

    @Test
    void shouldCreateCartWithItems() {
        Long customerId = persistTestCustomer();

        Cart cart = Cart.newCart(customerId);
        cart.addItem(buildCartItem());

        Cart saved = cartJpaAdapter.save(cart);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getItems()).hasSize(1);
        assertThat(saved.getItems().get(0).getProductName()).isEqualTo("Laptop");
        assertThat(saved.getItems().get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    void shouldCalculateCartTotal() {
        Long customerId = persistTestCustomer();

        Cart cart = Cart.newCart(customerId);
        cart.addItem(buildCartItem());
        cart.addItem(CartItem.builder()
                .id(UUID.randomUUID().toString())
                .productId(2L)
                .productName("Mouse")
                .productSku("MOU-001")
                .unitPrice(Money.of(BigDecimal.valueOf(25)))
                .quantity(3)
                .build());

        Cart saved = cartJpaAdapter.save(cart);

        Cart found = cartJpaAdapter.findById(saved.getId()).orElseThrow();

        assertThat(found.total().amount()).isEqualByComparingTo(BigDecimal.valueOf(2073));
    }

    @Test
    void shouldFindCartById() {
        Long customerId = persistTestCustomer();

        Cart cart = cartJpaAdapter.save(Cart.newCart(customerId));

        Optional<Cart> found = cartJpaAdapter.findById(cart.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getCustomerId()).isEqualTo(customerId);
    }

    @Test
    void shouldFindActiveCartByCustomerId() {
        Long customerId = persistTestCustomer();

        cartJpaAdapter.save(Cart.newCart(customerId));

        Optional<Cart> found = cartJpaAdapter.findActiveByCustomerId(customerId);

        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(CartStatus.ACTIVE);
    }

    @Test
    void shouldRemoveItemFromCart() {
        Long customerId = persistTestCustomer();

        Cart cart = Cart.newCart(customerId);
        CartItem item = buildCartItem();
        cart.addItem(item);

        Cart saved = cartJpaAdapter.save(cart);
        assertThat(saved.getItems()).hasSize(1);

        saved.removeItem(item.getId());
        Cart updated = cartJpaAdapter.save(saved);

        assertThat(updated.getItems()).isEmpty();
    }

    @Test
    void shouldConvertCart() {
        Long customerId = persistTestCustomer();

        Cart cart = cartJpaAdapter.save(Cart.newCart(customerId));

        cart.convert();
        Cart converted = cartJpaAdapter.save(cart);

        assertThat(converted.getStatus()).isEqualTo(CartStatus.CONVERTED);
    }
}