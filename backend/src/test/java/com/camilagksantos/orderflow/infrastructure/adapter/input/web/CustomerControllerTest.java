package com.camilagksantos.orderflow.infrastructure.adapter.input.web;

import com.camilagksantos.orderflow.BaseIntegrationTest;
import com.camilagksantos.orderflow.application.dto.request.RegisterCustomerRequest;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@Transactional
class CustomerControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper objectMapper;

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

    @Test
    void shouldRegisterCustomerWithoutAuthentication() throws Exception {
        RegisterCustomerRequest request = new RegisterCustomerRequest(
                "Maria Silva", "maria-" + UUID.randomUUID() + "@example.com",
                "123456789", "912345678", "Password123"
        );

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Maria Silva"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void shouldRejectRegisterWithInvalidEmail() throws Exception {
        RegisterCustomerRequest request = new RegisterCustomerRequest(
                "Joao Santos", "not-an-email", "123456789", "913456789", "Password123"
        );

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectRegisterWithInvalidNif() throws Exception {
        RegisterCustomerRequest request = new RegisterCustomerRequest(
                "Ana Costa", "ana-" + UUID.randomUUID() + "@example.com",
                "12", "914567890", "Password123"
        );

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectDuplicateEmailRegistration() throws Exception {
        String email = "duplicate-" + UUID.randomUUID() + "@example.com";
        RegisterCustomerRequest first = new RegisterCustomerRequest(
                "Pedro Alves", email, "123456789", "915678901", "Password123"
        );

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        RegisterCustomerRequest second = new RegisterCustomerRequest(
                "Pedro Alves Duplicado", email, "123456789", "915678902", "Password123"
        );

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRejectFindCustomerByIdWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/customers/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldFindCustomerByIdWithValidToken() throws Exception {
        String token = createUserAndGetToken("CUSTOMER");

        RegisterCustomerRequest request = new RegisterCustomerRequest(
                "Rita Ferreira", "rita-" + UUID.randomUUID() + "@example.com",
                "123456789", "916789012", "Password123"
        );

        String response = mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/v1/customers/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Rita Ferreira"));
    }

    @Test
    void shouldReturnNotFoundForMissingCustomer() throws Exception {
        String token = createUserAndGetToken("CUSTOMER");

        mockMvc.perform(get("/api/v1/customers/999999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}