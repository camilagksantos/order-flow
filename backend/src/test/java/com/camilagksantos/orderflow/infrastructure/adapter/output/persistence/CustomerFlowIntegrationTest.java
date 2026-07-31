package com.camilagksantos.orderflow.infrastructure.adapter.output.persistence;

import com.camilagksantos.orderflow.BaseIntegrationTest;
import com.camilagksantos.orderflow.domain.customer.Address;
import com.camilagksantos.orderflow.domain.customer.Customer;
import com.camilagksantos.orderflow.domain.customer.CustomerStatus;
import com.camilagksantos.orderflow.domain.shared.Email;
import com.camilagksantos.orderflow.domain.shared.NIF;
import com.camilagksantos.orderflow.infrastructure.persistence.entity.UserEntity;
import com.camilagksantos.orderflow.infrastructure.persistence.repository.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@Transactional
class CustomerFlowIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private CustomerJpaAdapter customerJpaAdapter;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UserEntity savedUser;

    @BeforeEach
    void setUp() {
        UserEntity user = new UserEntity();
        user.setEmail("camila@test.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setActive(true);
        user.setRoles(new ArrayList<>());
        savedUser = userJpaRepository.save(user);
    }

    @Test
    void shouldRegisterCustomer() {
        Customer customer = Customer.builder()
                .userId(savedUser.getId())
                .name("Camila Kfouri")
                .email(new Email("camila@test.com"))
                .nif(new NIF("123456789"))
                .phone("+351 912 345 678")
                .status(CustomerStatus.ACTIVE)
                .addresses(new ArrayList<>())
                .build();

        Customer saved = customerJpaAdapter.save(customer);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Camila Kfouri");
        assertThat(saved.getEmail().value()).isEqualTo("camila@test.com");
        assertThat(saved.getNif().value()).isEqualTo("123456789");
    }

    @Test
    void shouldFindCustomerById() {
        Customer customer = customerJpaAdapter.save(Customer.builder()
                .userId(savedUser.getId())
                .name("Camila Kfouri")
                .email(new Email("camila@test.com"))
                .nif(new NIF("123456789"))
                .phone("+351 912 345 678")
                .status(CustomerStatus.ACTIVE)
                .addresses(new ArrayList<>())
                .build());

        Optional<Customer> found = customerJpaAdapter.findById(customer.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Camila Kfouri");
    }

    @Test
    void shouldFindCustomerByEmail() {
        customerJpaAdapter.save(Customer.builder()
                .userId(savedUser.getId())
                .name("Camila Kfouri")
                .email(new Email("camila@test.com"))
                .nif(new NIF("123456789"))
                .status(CustomerStatus.ACTIVE)
                .addresses(new ArrayList<>())
                .build());

        Optional<Customer> found = customerJpaAdapter.findByEmail("camila@test.com");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail().value()).isEqualTo("camila@test.com");
    }

    @Test
    void shouldFindCustomerByNif() {
        customerJpaAdapter.save(Customer.builder()
                .userId(savedUser.getId())
                .name("Camila Kfouri")
                .email(new Email("camila@test.com"))
                .nif(new NIF("123456789"))
                .status(CustomerStatus.ACTIVE)
                .addresses(new ArrayList<>())
                .build());

        Optional<Customer> found = customerJpaAdapter.findByNif("123456789");

        assertThat(found).isPresent();
        assertThat(found.get().getNif().value()).isEqualTo("123456789");
    }

    @Test
    void shouldReturnEmptyWhenCustomerNotFound() {
        Optional<Customer> found = customerJpaAdapter.findById(999L);
        assertThat(found).isEmpty();
    }
}