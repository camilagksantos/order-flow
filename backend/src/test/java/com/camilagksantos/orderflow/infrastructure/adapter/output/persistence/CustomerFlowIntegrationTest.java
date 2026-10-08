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
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

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

    @PersistenceContext
    private EntityManager entityManager;

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

    private Address address(String street) {
        return Address.builder()
                .street(street)
                .number("10")
                .neighborhood("Baixa")
                .city("Lisboa")
                .district("Lisboa")
                .postalCode("1100-000")
                .country("PT")
                .build();
    }

    private Customer saveCustomerWithAddresses(String... streets) {
        Customer customer = Customer.builder()
                .userId(savedUser.getId())
                .name("Camila Kfouri")
                .email(new Email("camila@test.com"))
                .nif(new NIF("123456789"))
                .status(CustomerStatus.ACTIVE)
                .addresses(new ArrayList<>())
                .build();
        for (String street : streets) {
            customer.addAddress(address(street));
        }
        return customerJpaAdapter.save(customer);
    }

    private Customer reload(Long id) {
        entityManager.flush();
        entityManager.clear();
        return customerJpaAdapter.findById(id).orElseThrow();
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

    @Test
    void shouldSaveAddressesAndMakeOnlyTheFirstOneDefault() {
        Customer saved = saveCustomerWithAddresses("Rua A", "Rua B");

        Customer found = reload(saved.getId());

        assertThat(found.getAddresses()).hasSize(2);
        assertThat(found.getAddresses())
                .filteredOn(Address::isDefaultAddress)
                .extracting(Address::getStreet)
                .containsExactly("Rua A");
    }

    @Test
    void shouldAddAnAddressToAnExistingCustomer() {
        Customer saved = saveCustomerWithAddresses("Rua A");
        Customer found = reload(saved.getId());
        found.addAddress(address("Rua B"));
        customerJpaAdapter.save(found);

        Customer reloaded = reload(saved.getId());

        assertThat(reloaded.getAddresses()).hasSize(2);
        assertThat(reloaded.getAddresses()).allSatisfy(address -> assertThat(address.getId()).isNotNull());
    }

    @Test
    void shouldDeleteTheRowOfARemovedAddress() {
        Customer saved = saveCustomerWithAddresses("Rua A", "Rua B");
        Customer found = reload(saved.getId());
        Long removedId = found.getAddresses().stream()
                .filter(address -> "Rua B".equals(address.getStreet()))
                .findFirst().orElseThrow().getId();
        found.removeAddress(removedId);
        customerJpaAdapter.save(found);

        Customer reloaded = reload(saved.getId());

        assertThat(reloaded.getAddresses()).hasSize(1);
        assertThat(reloaded.getAddresses().get(0).getStreet()).isEqualTo("Rua A");
    }

    @Test
    void shouldPersistTheNewDefaultAddress() {
        Customer saved = saveCustomerWithAddresses("Rua A", "Rua B");
        Customer found = reload(saved.getId());
        Long otherId = found.getAddresses().stream()
                .filter(address -> "Rua B".equals(address.getStreet()))
                .findFirst().orElseThrow().getId();
        found.makeAddressDefault(otherId);
        customerJpaAdapter.save(found);

        Customer reloaded = reload(saved.getId());

        assertThat(reloaded.getAddresses())
                .filteredOn(Address::isDefaultAddress)
                .extracting(Address::getStreet)
                .containsExactly("Rua B");
    }

    @Test
    void shouldPersistEditedAddressFields() {
        Customer saved = saveCustomerWithAddresses("Rua A");
        Customer found = reload(saved.getId());
        Long addressId = found.getAddresses().get(0).getId();
        Address data = address("Rua Nova");
        data.setPostalCode("4000-001");
        found.updateAddress(addressId, data);
        customerJpaAdapter.save(found);

        Customer reloaded = reload(saved.getId());

        assertThat(reloaded.getAddresses()).hasSize(1);
        assertThat(reloaded.getAddresses().get(0).getStreet()).isEqualTo("Rua Nova");
        assertThat(reloaded.getAddresses().get(0).getPostalCode()).isEqualTo("4000-001");
        assertThat(reloaded.getAddresses().get(0).getCountry()).isEqualTo("PT");
    }
}