package com.camilagksantos.orderflow.infrastructure.adapter.input.web;

import com.camilagksantos.orderflow.BaseIntegrationTest;
import com.camilagksantos.orderflow.application.dto.request.CreateProductRequest;
import com.camilagksantos.orderflow.application.dto.request.UpdateProductRequest;
import com.camilagksantos.orderflow.domain.category.Category;
import com.camilagksantos.orderflow.infrastructure.adapter.output.persistence.CategoryJpaAdapter;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@Transactional
class ProductControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

    @Autowired
    private CategoryJpaAdapter categoryJpaAdapter;

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

    private Long persistTestCategory() {
        return categoryJpaAdapter.save(Category.builder().name("Electronics").build()).getId();
    }

    @Test
    void shouldCreateProductAsAdmin() throws Exception {
        String token = createUserAndGetToken("ADMIN");
        Long categoryId = persistTestCategory();

        CreateProductRequest request = new CreateProductRequest(
                "Laptop", "A powerful laptop", "LAP-" + UUID.randomUUID(),
                BigDecimal.valueOf(999.99), 10, categoryId, null
        );

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Laptop"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void shouldRejectCreateProductAsCustomer() throws Exception {
        String token = createUserAndGetToken("CUSTOMER");
        Long categoryId = persistTestCategory();

        CreateProductRequest request = new CreateProductRequest(
                "Mouse", "Wireless mouse", "MOU-" + UUID.randomUUID(),
                BigDecimal.valueOf(25), 50, categoryId, null
        );

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectCreateProductWithInvalidPrice() throws Exception {
        String token = createUserAndGetToken("ADMIN");
        Long categoryId = persistTestCategory();

        CreateProductRequest request = new CreateProductRequest(
                "Keyboard", "Mechanical keyboard", "KEY-" + UUID.randomUUID(),
                BigDecimal.ZERO, 5, categoryId, null
        );

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldFindAllProductsWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void shouldFindProductByIdWithoutAuthentication() throws Exception {
        String token = createUserAndGetToken("ADMIN");
        Long categoryId = persistTestCategory();
        String sku = "MON-" + UUID.randomUUID();

        String response = mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateProductRequest(
                                "Monitor", "4K monitor", sku, BigDecimal.valueOf(400), 15, categoryId, null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/v1/products/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value(sku));
    }

    @Test
    void shouldReturnNotFoundForMissingProduct() throws Exception {
        mockMvc.perform(get("/api/v1/products/999999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldFindProductBySku() throws Exception {
        String token = createUserAndGetToken("ADMIN");
        Long categoryId = persistTestCategory();
        String sku = "TAB-" + UUID.randomUUID();

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateProductRequest(
                                "Tablet", "Android tablet", sku, BigDecimal.valueOf(300), 8, categoryId, null))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/products/sku/" + sku))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value(sku));
    }

    @Test
    void shouldFindProductsByCategoryId() throws Exception {
        String token = createUserAndGetToken("ADMIN");
        Long categoryId = persistTestCategory();

        mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateProductRequest(
                                "Headset", "Gaming headset", "HDS-" + UUID.randomUUID(),
                                BigDecimal.valueOf(75), 20, categoryId, null))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/products/category/" + categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void shouldUpdateProductAsAdmin() throws Exception {
        String token = createUserAndGetToken("ADMIN");
        Long categoryId = persistTestCategory();

        String response = mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateProductRequest(
                                "Speaker", "Bluetooth speaker", "SPK-" + UUID.randomUUID(),
                                BigDecimal.valueOf(50), 12, categoryId, null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        UpdateProductRequest updateRequest = new UpdateProductRequest(
                "Speaker Pro", "Updated bluetooth speaker", BigDecimal.valueOf(60), 20, categoryId, null
        );

        mockMvc.perform(put("/api/v1/products/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Speaker Pro"));
    }

    @Test
    void shouldRejectUpdateProductAsCustomer() throws Exception {
        String adminToken = createUserAndGetToken("ADMIN");
        String customerToken = createUserAndGetToken("CUSTOMER");
        Long categoryId = persistTestCategory();

        String response = mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateProductRequest(
                                "Webcam", "HD webcam", "WEB-" + UUID.randomUUID(),
                                BigDecimal.valueOf(45), 30, categoryId, null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        UpdateProductRequest updateRequest = new UpdateProductRequest(
                "Webcam Pro", "Updated webcam", BigDecimal.valueOf(55), 25, categoryId, null
        );

        mockMvc.perform(put("/api/v1/products/" + id)
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldDeleteProductAsAdmin() throws Exception {
        String token = createUserAndGetToken("ADMIN");
        Long categoryId = persistTestCategory();

        String response = mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateProductRequest(
                                "Charger", "USB-C charger", "CHG-" + UUID.randomUUID(),
                                BigDecimal.valueOf(20), 40, categoryId, null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(delete("/api/v1/products/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/products/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectDeleteProductAsCustomer() throws Exception {
        String adminToken = createUserAndGetToken("ADMIN");
        String customerToken = createUserAndGetToken("CUSTOMER");
        Long categoryId = persistTestCategory();

        String response = mockMvc.perform(post("/api/v1/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateProductRequest(
                                "Cable", "HDMI cable", "CBL-" + UUID.randomUUID(),
                                BigDecimal.valueOf(10), 60, categoryId, null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(delete("/api/v1/products/" + id)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }
}