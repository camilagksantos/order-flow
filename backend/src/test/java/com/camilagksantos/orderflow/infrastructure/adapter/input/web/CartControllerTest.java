package com.camilagksantos.orderflow.infrastructure.adapter.input.web;

import com.camilagksantos.orderflow.BaseIntegrationTest;
import com.camilagksantos.orderflow.application.dto.request.AddToCartRequest;
import com.camilagksantos.orderflow.application.dto.request.CheckoutRequest;
import com.camilagksantos.orderflow.domain.category.Category;
import com.camilagksantos.orderflow.domain.customer.Customer;
import com.camilagksantos.orderflow.domain.customer.CustomerStatus;
import com.camilagksantos.orderflow.domain.order.PaymentMethod;
import com.camilagksantos.orderflow.domain.product.Product;
import com.camilagksantos.orderflow.domain.product.ProductStatus;
import com.camilagksantos.orderflow.domain.shared.Email;
import com.camilagksantos.orderflow.domain.shared.Money;
import com.camilagksantos.orderflow.domain.shared.NIF;
import com.camilagksantos.orderflow.infrastructure.adapter.output.persistence.CategoryJpaAdapter;
import com.camilagksantos.orderflow.infrastructure.adapter.output.persistence.CustomerJpaAdapter;
import com.camilagksantos.orderflow.infrastructure.adapter.output.persistence.ProductJpaAdapter;
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
class CartControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @Autowired
    private CustomerJpaAdapter customerJpaAdapter;

    @Autowired
    private CategoryJpaAdapter categoryJpaAdapter;

    @Autowired
    private ProductJpaAdapter productJpaAdapter;

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

    private String token;
    private Long customerId;

    private void setUpCustomerWithToken() {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        RoleEntity role = roleJpaRepository.findByName("CUSTOMER").orElseThrow();

        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("Password123"));
        user.setActive(true);
        user.setRoles(List.of(role));
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
        customerId = customerJpaAdapter.save(customer).getId();

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        token = jwtService.generateAccessToken(userDetails, Map.of());
    }

    private Long persistTestProduct() {
        Category category = categoryJpaAdapter.save(Category.builder().name("Electronics").build());
        Product product = Product.builder()
                .name("Laptop")
                .sku("LAP-" + UUID.randomUUID())
                .price(Money.of(BigDecimal.valueOf(999)))
                .stockQuantity(10)
                .reservedQuantity(0)
                .category(category)
                .status(ProductStatus.ACTIVE)
                .build();
        return productJpaAdapter.save(product).getId();
    }

    @Test
    void shouldRejectAddItemWithoutToken() throws Exception {
        setUpCustomerWithToken();
        AddToCartRequest request = new AddToCartRequest(1L, 2);

        mockMvc.perform(post("/api/v1/carts/customer/" + customerId + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAddItemToCart() throws Exception {
        setUpCustomerWithToken();
        Long productId = persistTestProduct();
        AddToCartRequest request = new AddToCartRequest(productId, 2);

        mockMvc.perform(post("/api/v1/carts/customer/" + customerId + "/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(customerId))
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void shouldRejectAddItemWithInvalidQuantity() throws Exception {
        setUpCustomerWithToken();
        Long productId = persistTestProduct();
        AddToCartRequest request = new AddToCartRequest(productId, 0);

        mockMvc.perform(post("/api/v1/carts/customer/" + customerId + "/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFindCartByCustomerId() throws Exception {
        setUpCustomerWithToken();
        Long productId = persistTestProduct();

        mockMvc.perform(post("/api/v1/carts/customer/" + customerId + "/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddToCartRequest(productId, 1))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/carts/customer/" + customerId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(customerId));
    }

    @Test
    void shouldRemoveItemFromCart() throws Exception {
        setUpCustomerWithToken();
        Long productId = persistTestProduct();

        String response = mockMvc.perform(post("/api/v1/carts/customer/" + customerId + "/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddToCartRequest(productId, 1))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String itemId = objectMapper.readTree(response).get("items").get(0).get("id").asText();

        mockMvc.perform(delete("/api/v1/carts/customer/" + customerId + "/items/" + itemId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void shouldCheckoutCart() throws Exception {
        setUpCustomerWithToken();
        Long productId = persistTestProduct();

        mockMvc.perform(post("/api/v1/carts/customer/" + customerId + "/items")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddToCartRequest(productId, 1))))
                .andExpect(status().isCreated());

        CheckoutRequest checkoutRequest = new CheckoutRequest(
                UUID.randomUUID().toString(), 1L, PaymentMethod.MBWAY
        );

        mockMvc.perform(post("/api/v1/carts/customer/" + customerId + "/checkout")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkoutRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.paymentMethod").value("MBWAY"));
    }
}