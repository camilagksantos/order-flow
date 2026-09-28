package com.camilagksantos.orderflow.infrastructure.adapter.input.web;

import com.camilagksantos.orderflow.BaseIntegrationTest;
import com.camilagksantos.orderflow.application.dto.request.ProcessPaymentRequest;
import com.camilagksantos.orderflow.domain.cart.Cart;
import com.camilagksantos.orderflow.domain.cart.CartItem;
import com.camilagksantos.orderflow.domain.customer.Customer;
import com.camilagksantos.orderflow.domain.customer.CustomerStatus;
import com.camilagksantos.orderflow.domain.order.PaymentMethod;
import com.camilagksantos.orderflow.domain.order.ShopOrder;
import com.camilagksantos.orderflow.domain.shared.Email;
import com.camilagksantos.orderflow.domain.shared.Money;
import com.camilagksantos.orderflow.domain.shared.NIF;
import com.camilagksantos.orderflow.infrastructure.adapter.output.persistence.CustomerJpaAdapter;
import com.camilagksantos.orderflow.infrastructure.adapter.output.persistence.OrderJpaAdapter;
import com.camilagksantos.orderflow.infrastructure.config.security.JwtService;
import com.camilagksantos.orderflow.infrastructure.config.security.UserDetailsServiceImpl;
import com.camilagksantos.orderflow.infrastructure.persistence.entity.RoleEntity;
import com.camilagksantos.orderflow.infrastructure.persistence.entity.UserEntity;
import com.camilagksantos.orderflow.infrastructure.persistence.repository.RoleJpaRepository;
import com.camilagksantos.orderflow.infrastructure.persistence.repository.UserJpaRepository;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@Transactional
class PaymentControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @Autowired
    private CustomerJpaAdapter customerJpaAdapter;

    @Autowired
    private OrderJpaAdapter orderJpaAdapter;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private RoleJpaRepository roleJpaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    private String createCustomerToken(Long customerId) {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        RoleEntity role = roleJpaRepository.findByName("CUSTOMER").orElseThrow();

        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("Password123"));
        user.setActive(true);
        user.setRoles(List.of(role));
        userJpaRepository.save(user);

        Map<String, Object> claims = new HashMap<>();
        if (customerId != null) {
            claims.put("customerId", customerId);
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        return jwtService.generateAccessToken(userDetails, claims);
    }

    private Customer persistTestCustomer() {
        String email = "customer-" + UUID.randomUUID() + "@example.com";
        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("Password123"));
        user.setActive(true);
        user.setRoles(List.of());
        Long userId = userJpaRepository.save(user).getId();

        Customer customer = Customer.builder()
                .userId(userId)
                .name("Test Customer")
                .email(new Email(email))
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
                .quantity(1)
                .build());

        ShopOrder order = ShopOrder.fromCart(cart, UUID.randomUUID().toString(),
                customer.getEmail().value(), PaymentMethod.CREDIT_CARD);
        return orderJpaAdapter.save(order);
    }

    private ProcessPaymentRequest cardPayment(String orderId) {
        return new ProcessPaymentRequest(orderId, PaymentMethod.CREDIT_CARD, "4242", "VISA", null, null, null);
    }

    @Test
    void shouldRejectPaymentWithoutToken() throws Exception {
        Customer customer = persistTestCustomer();
        ShopOrder order = persistTestOrder(customer);

        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cardPayment(order.getId()))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldProcessPaymentAndMarkOrderPaid() throws Exception {
        Customer customer = persistTestCustomer();
        String token = createCustomerToken(customer.getId());
        ShopOrder order = persistTestOrder(customer);

        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cardPayment(order.getId()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.orderId").value(order.getId()))
                .andExpect(jsonPath("$.transactionId").isNotEmpty());

        mockMvc.perform(get("/api/v1/orders/" + order.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void shouldRejectPaymentForAnotherCustomersOrder() throws Exception {
        Customer customer = persistTestCustomer();
        String token = createCustomerToken(customer.getId() + 1);
        ShopOrder order = persistTestOrder(customer);

        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cardPayment(order.getId()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectSecondPaymentForSameOrder() throws Exception {
        Customer customer = persistTestCustomer();
        String token = createCustomerToken(customer.getId());
        ShopOrder order = persistTestOrder(customer);

        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cardPayment(order.getId()))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cardPayment(order.getId()))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void shouldRejectPaymentWithDifferentMethodFromOrder() throws Exception {
        Customer customer = persistTestCustomer();
        String token = createCustomerToken(customer.getId());
        ShopOrder order = persistTestOrder(customer);

        ProcessPaymentRequest request = new ProcessPaymentRequest(
                order.getId(), PaymentMethod.MBWAY, null, null, "912345678", null, null);

        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void shouldReturnNotFoundForMissingOrder() throws Exception {
        String token = createCustomerToken(null);

        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cardPayment("non-existent-id"))))
                .andExpect(status().isNotFound());
    }
}