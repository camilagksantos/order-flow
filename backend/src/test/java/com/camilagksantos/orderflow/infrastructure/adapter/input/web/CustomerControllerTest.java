package com.camilagksantos.orderflow.infrastructure.adapter.input.web;

import com.camilagksantos.orderflow.BaseIntegrationTest;
import com.camilagksantos.orderflow.application.dto.request.CreateAddressRequest;
import com.camilagksantos.orderflow.application.dto.request.RegisterCustomerRequest;
import com.camilagksantos.orderflow.application.dto.request.UpdateAddressRequest;
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

import java.util.HashMap;
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
        return createUserAndGetToken(roleName, null);
    }

    private String createUserAndGetToken(String roleName, Long customerId) {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        RoleEntity role = roleJpaRepository.findByName(roleName).orElseThrow();

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

    private Long registerCustomerAndGetId(String name) throws Exception {
        RegisterCustomerRequest request = new RegisterCustomerRequest(
                name, "customer-" + UUID.randomUUID() + "@example.com",
                "123456789", "916789012", "Password123"
        );

        String response = mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    private CreateAddressRequest addressRequest(String street) {
        return new CreateAddressRequest(street, "10", null, "Baixa", "Lisboa", "Lisboa", "1100-000");
    }

    private Long createAddressAndGetId(Long customerId, String token, String street) throws Exception {
        String response = mockMvc.perform(post("/api/v1/customers/" + customerId + "/addresses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addressRequest(street))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
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
    void shouldFindOwnCustomerById() throws Exception {
        Long id = registerCustomerAndGetId("Rita Ferreira");
        String token = createUserAndGetToken("CUSTOMER", id);

        mockMvc.perform(get("/api/v1/customers/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Rita Ferreira"));
    }

    @Test
    void shouldRejectFindCustomerOfAnotherCustomer() throws Exception {
        Long id = registerCustomerAndGetId("Rita Ferreira");
        String token = createUserAndGetToken("CUSTOMER", id + 1);

        mockMvc.perform(get("/api/v1/customers/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowAdminToFindAnyCustomer() throws Exception {
        Long id = registerCustomerAndGetId("Rita Ferreira");
        String token = createUserAndGetToken("ADMIN");

        mockMvc.perform(get("/api/v1/customers/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Rita Ferreira"));
    }

    @Test
    void shouldReturnNotFoundForMissingCustomer() throws Exception {
        String token = createUserAndGetToken("ADMIN");

        mockMvc.perform(get("/api/v1/customers/999999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreateAddressForTheOwnerAndMakeTheFirstOneDefault() throws Exception {
        Long customerId = registerCustomerAndGetId("Rita Ferreira");
        String token = createUserAndGetToken("CUSTOMER", customerId);

        mockMvc.perform(post("/api/v1/customers/" + customerId + "/addresses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addressRequest("Rua A"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.street").value("Rua A"))
                .andExpect(jsonPath("$.country").value("PT"))
                .andExpect(jsonPath("$.defaultAddress").value(true));
    }

    @Test
    void shouldRejectCreateAddressForAnotherCustomer() throws Exception {
        Long customerId = registerCustomerAndGetId("Rita Ferreira");
        String token = createUserAndGetToken("CUSTOMER", customerId + 1);

        mockMvc.perform(post("/api/v1/customers/" + customerId + "/addresses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addressRequest("Rua A"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectCreateAddressWithInvalidPostalCode() throws Exception {
        Long customerId = registerCustomerAndGetId("Rita Ferreira");
        String token = createUserAndGetToken("CUSTOMER", customerId);
        CreateAddressRequest invalid = new CreateAddressRequest("Rua A", "10", null, "Baixa", "Lisboa", "Lisboa", "1100");

        mockMvc.perform(post("/api/v1/customers/" + customerId + "/addresses")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectCreateAddressWithoutToken() throws Exception {
        mockMvc.perform(post("/api/v1/customers/1/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addressRequest("Rua A"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldUpdateAddress() throws Exception {
        Long customerId = registerCustomerAndGetId("Rita Ferreira");
        String token = createUserAndGetToken("CUSTOMER", customerId);
        Long addressId = createAddressAndGetId(customerId, token, "Rua A");
        UpdateAddressRequest request = new UpdateAddressRequest("Rua Nova", "5", "2A", "Chiado", "Porto", "Porto", "4000-001");

        mockMvc.perform(put("/api/v1/customers/" + customerId + "/addresses/" + addressId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.street").value("Rua Nova"))
                .andExpect(jsonPath("$.city").value("Porto"))
                .andExpect(jsonPath("$.postalCode").value("4000-001"))
                .andExpect(jsonPath("$.country").value("PT"))
                .andExpect(jsonPath("$.defaultAddress").value(true));
    }

    @Test
    void shouldReturnNotFoundWhenUpdatingAnUnknownAddress() throws Exception {
        Long customerId = registerCustomerAndGetId("Rita Ferreira");
        String token = createUserAndGetToken("CUSTOMER", customerId);
        createAddressAndGetId(customerId, token, "Rua A");
        UpdateAddressRequest request = new UpdateAddressRequest("Rua Nova", "5", null, "Chiado", "Porto", "Porto", "4000-001");

        mockMvc.perform(put("/api/v1/customers/" + customerId + "/addresses/999999999")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldChangeTheDefaultAddress() throws Exception {
        Long customerId = registerCustomerAndGetId("Rita Ferreira");
        String token = createUserAndGetToken("CUSTOMER", customerId);
        createAddressAndGetId(customerId, token, "Rua A");
        Long secondId = createAddressAndGetId(customerId, token, "Rua B");

        mockMvc.perform(patch("/api/v1/customers/" + customerId + "/addresses/" + secondId + "/default")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaultAddress").value(true));

        mockMvc.perform(get("/api/v1/customers/" + customerId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addresses[0].defaultAddress").value(false))
                .andExpect(jsonPath("$.addresses[1].defaultAddress").value(true));
    }

    @Test
    void shouldDeleteAnAddress() throws Exception {
        Long customerId = registerCustomerAndGetId("Rita Ferreira");
        String token = createUserAndGetToken("CUSTOMER", customerId);
        createAddressAndGetId(customerId, token, "Rua A");
        Long secondId = createAddressAndGetId(customerId, token, "Rua B");

        mockMvc.perform(delete("/api/v1/customers/" + customerId + "/addresses/" + secondId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/customers/" + customerId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addresses.length()").value(1));
    }

    @Test
    void shouldRejectDeletingTheLastAddress() throws Exception {
        Long customerId = registerCustomerAndGetId("Rita Ferreira");
        String token = createUserAndGetToken("CUSTOMER", customerId);
        Long onlyId = createAddressAndGetId(customerId, token, "Rua A");

        mockMvc.perform(delete("/api/v1/customers/" + customerId + "/addresses/" + onlyId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void shouldRejectDeleteAddressOfAnotherCustomer() throws Exception {
        Long customerId = registerCustomerAndGetId("Rita Ferreira");
        String ownerToken = createUserAndGetToken("CUSTOMER", customerId);
        Long addressId = createAddressAndGetId(customerId, ownerToken, "Rua A");
        String otherToken = createUserAndGetToken("CUSTOMER", customerId + 1);

        mockMvc.perform(delete("/api/v1/customers/" + customerId + "/addresses/" + addressId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }
}