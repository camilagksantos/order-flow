package com.camilagksantos.orderflow.infrastructure.adapter.input.web;

import com.camilagksantos.orderflow.BaseIntegrationTest;
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
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Autowired
    private CustomerJpaAdapter customerJpaAdapter;

    @Autowired
    private OrderJpaAdapter orderJpaAdapter;

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

    private void persistTestOrder(Customer customer, BigDecimal amount) {
        Cart cart = Cart.newCart(customer.getId());
        cart.addItem(CartItem.builder()
                .id(UUID.randomUUID().toString())
                .productId(1L)
                .productName("Laptop")
                .productSku("LAP-001")
                .unitPrice(Money.of(amount))
                .quantity(1)
                .build());

        ShopOrder order = ShopOrder.fromCart(cart, UUID.randomUUID().toString(),
                customer.getEmail().value(), PaymentMethod.CREDIT_CARD);
        orderJpaAdapter.save(order);
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

    @Test
    void shouldReturnValidXlsxFileWithCorrectHeaders() throws Exception {
        String token = createUserAndGetToken("ADMIN");
        Customer customer = persistTestCustomer();
        persistTestOrder(customer, BigDecimal.valueOf(500));

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/reports/sales")
                        .header("Authorization", "Bearer " + token)
                        .param("startDate", "2020-01-01")
                        .param("endDate", "2030-12-31"))
                .andExpect(status().isOk())
                .andReturn().getResponse();

        assertThat(response.getContentType()).isEqualTo(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThat(response.getHeader("Content-Disposition"))
                .contains("attachment")
                .contains("sales-report.xlsx");

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(2);
        }
    }

    @Test
    void shouldReflectPersistedOrdersInReport() throws Exception {
        String token = createUserAndGetToken("ADMIN");
        Customer customer = persistTestCustomer();
        persistTestOrder(customer, BigDecimal.valueOf(300));

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/reports/sales")
                        .header("Authorization", "Bearer " + token)
                        .param("startDate", "2020-01-01")
                        .param("endDate", "2030-12-31"))
                .andExpect(status().isOk())
                .andReturn().getResponse();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            var ordersSheet = workbook.getSheet("Orders");
            assertThat(ordersSheet.getLastRowNum()).isGreaterThanOrEqualTo(1);
            assertThat(ordersSheet.getRow(1).getCell(1).getStringCellValue())
                    .isEqualTo(customer.getEmail().value());
        }
    }
}