package com.camilagksantos.orderflow.infrastructure.adapter.input.web;

import com.camilagksantos.orderflow.BaseIntegrationTest;
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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Transactional
class ReportControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
    void shouldGenerateSalesReportAsAdmin() throws Exception {
        String token = createUserAndGetToken("ADMIN");

        mockMvc.perform(get("/api/v1/reports/sales")
                        .header("Authorization", "Bearer " + token)
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-12-31"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectSalesReportAsCustomer() throws Exception {
        String token = createUserAndGetToken("CUSTOMER");

        mockMvc.perform(get("/api/v1/reports/sales")
                        .header("Authorization", "Bearer " + token)
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-12-31"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectSalesReportWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/reports/sales")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-12-31"))
                .andExpect(status().isUnauthorized());
    }
}