package com.camilagksantos.orderflow.infrastructure.adapter.input.web;

import com.camilagksantos.orderflow.BaseIntegrationTest;
import com.camilagksantos.orderflow.application.dto.request.CancelOrderRequest;
import com.camilagksantos.orderflow.application.dto.request.UpdateOrderStatusRequest;
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
import com.camilagksantos.orderflow.infrastructure.adapter.output.persistence.CustomerJpaAdapter;
import com.camilagksantos.orderflow.infrastructure.adapter.output.persistence.OrderJpaAdapter;
import com.camilagksantos.orderflow.infrastructure.config.security.JwtService;
import com.camilagksantos.orderflow.infrastructure.config.security.UserDetailsServiceImpl;
import com.camilagksantos.orderflow.infrastructure.persistence.entity.RoleEntity;
import com.camilagksantos.orderflow.infrastructure.persistence.entity.UserEntity;
import com.camilagksantos.orderflow.infrastructure.persistence.repository.RoleJpaRepository;
import com.camilagksantos.orderflow.infrastructure.persistence.repository.UserJpaRepository;
import tools.jackson.databind.json.JsonMapper;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@Transactional
class OrderControllerTest extends BaseIntegrationTest {

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

    private String createUserAndGetToken(String roleName) {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        RoleEntity role = roleJpaRepository.findByName(roleName).orElseThrow();

        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("Password123"));
        user.setActive(true);
        user.setRoles(List.of(role));
        userJpaRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        return jwtService.generateAccessToken(userDetails, Map.of());
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

    @Test
    void shouldRejectFindOrderByIdWithoutToken() throws Exception {
        Customer customer = persistTestCustomer();
        ShopOrder order = persistTestOrder(customer);

        mockMvc.perform(get("/api/v1/orders/" + order.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldFindOrderById() throws Exception {
        String token = createUserAndGetToken("CUSTOMER");
        Customer customer = persistTestCustomer();
        ShopOrder order = persistTestOrder(customer);

        mockMvc.perform(get("/api/v1/orders/" + order.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value(order.getOrderNumber()));
    }

    @Test
    void shouldFindOrderByOrderNumber() throws Exception {
        String token = createUserAndGetToken("CUSTOMER");
        Customer customer = persistTestCustomer();
        ShopOrder order = persistTestOrder(customer);

        mockMvc.perform(get("/api/v1/orders/number/" + order.getOrderNumber())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order.getId()));
    }

    @Test
    void shouldFindOrdersByCustomerId() throws Exception {
        String token = createUserAndGetToken("CUSTOMER");
        Customer customer = persistTestCustomer();
        persistTestOrder(customer);
        persistTestOrder(customer);

        mockMvc.perform(get("/api/v1/orders/customer/" + customer.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldReturnNotFoundForMissingOrder() throws Exception {
        String token = createUserAndGetToken("CUSTOMER");

        mockMvc.perform(get("/api/v1/orders/non-existent-id")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldUpdateOrderStatusAsAdmin() throws Exception {
        String token = createUserAndGetToken("ADMIN");
        Customer customer = persistTestCustomer();
        ShopOrder order = persistTestOrder(customer);

        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.PAID);

        mockMvc.perform(patch("/api/v1/orders/" + order.getId() + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void shouldRejectUpdateOrderStatusAsCustomer() throws Exception {
        String token = createUserAndGetToken("CUSTOMER");
        Customer customer = persistTestCustomer();
        ShopOrder order = persistTestOrder(customer);

        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.PAID);

        mockMvc.perform(patch("/api/v1/orders/" + order.getId() + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldCancelOrder() throws Exception {
        String token = createUserAndGetToken("CUSTOMER");
        Customer customer = persistTestCustomer();
        ShopOrder order = persistTestOrder(customer);

        CancelOrderRequest request = new CancelOrderRequest("Changed my mind");

        mockMvc.perform(post("/api/v1/orders/" + order.getId() + "/cancel")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelReason").value("Changed my mind"));
    }

    @Test
    void shouldRejectCancelOrderWithBlankReason() throws Exception {
        String token = createUserAndGetToken("CUSTOMER");
        Customer customer = persistTestCustomer();
        ShopOrder order = persistTestOrder(customer);

        CancelOrderRequest request = new CancelOrderRequest("");

        mockMvc.perform(post("/api/v1/orders/" + order.getId() + "/cancel")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}