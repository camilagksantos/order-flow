package com.camilagksantos.orderflow.domain.customer;

import com.camilagksantos.orderflow.domain.exception.AddressNotFoundException;
import com.camilagksantos.orderflow.domain.exception.BusinessRuleException;
import com.camilagksantos.orderflow.domain.shared.Email;
import com.camilagksantos.orderflow.domain.shared.NIF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class CustomerTest {

    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .id(1L)
                .userId(1L)
                .name("Camila Kfouri")
                .email(new Email("camila@test.com"))
                .nif(new NIF("123456789"))
                .phone("+351 912 345 678")
                .status(CustomerStatus.ACTIVE)
                .addresses(List.of())
                .build();
    }

    private Address address(Long id, String street) {
        return Address.builder()
                .id(id)
                .street(street)
                .number("10")
                .neighborhood("Baixa")
                .city("Lisboa")
                .district("Lisboa")
                .postalCode("1100-000")
                .country("PT")
                .build();
    }

    private void givenTwoAddresses() {
        Address first = address(10L, "Rua A");
        first.setDefaultAddress(true);
        customer.setAddresses(new ArrayList<>(List.of(first, address(20L, "Rua B"))));
    }

    @Test
    void shouldBlockCustomer() {
        customer.block();
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.BLOCKED);
    }

    @Test
    void shouldActivateCustomer() {
        customer.block();
        customer.activate();
        assertThat(customer.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
    }

    @Test
    void shouldMakeTheFirstAddressTheDefault() {
        customer.addAddress(address(null, "Rua A"));

        assertThat(customer.getAddresses()).hasSize(1);
        assertThat(customer.getAddresses().get(0).isDefaultAddress()).isTrue();
        assertThat(customer.getAddresses().get(0).getCustomerId()).isEqualTo(1L);
    }

    @Test
    void shouldKeepTheDefaultWhenAddingASecondAddress() {
        customer.addAddress(address(null, "Rua A"));
        customer.addAddress(address(null, "Rua B"));

        assertThat(customer.getAddresses())
                .extracting(Address::isDefaultAddress)
                .containsExactly(true, false);
    }

    @Test
    void shouldAddTheFirstAddressWhenTheListIsNull() {
        customer.setAddresses(null);

        customer.addAddress(address(null, "Rua A"));

        assertThat(customer.getAddresses()).hasSize(1);
    }

    @Test
    void shouldUpdateAddressFields() {
        givenTwoAddresses();
        Address data = Address.builder()
                .street("Rua Nova")
                .number("5")
                .complement("2A")
                .neighborhood("Chiado")
                .city("Porto")
                .district("Porto")
                .postalCode("4000-001")
                .country("PT")
                .build();

        customer.updateAddress(10L, data);

        Address updated = customer.findAddress(10L);
        assertThat(updated.getStreet()).isEqualTo("Rua Nova");
        assertThat(updated.getCity()).isEqualTo("Porto");
        assertThat(updated.getDistrict()).isEqualTo("Porto");
        assertThat(updated.getPostalCode()).isEqualTo("4000-001");
        assertThat(updated.getId()).isEqualTo(10L);
        assertThat(updated.isDefaultAddress()).isTrue();
    }

    @Test
    void shouldThrowWhenUpdatingAnUnknownAddress() {
        givenTwoAddresses();

        assertThatThrownBy(() -> customer.updateAddress(99L, address(null, "Rua X")))
                .isInstanceOf(AddressNotFoundException.class);
    }

    @Test
    void shouldChangeTheDefaultAddress() {
        givenTwoAddresses();

        customer.makeAddressDefault(20L);

        assertThat(customer.getAddresses())
                .extracting(Address::isDefaultAddress)
                .containsExactly(false, true);
    }

    @Test
    void shouldRemoveANonDefaultAddressKeepingTheDefault() {
        givenTwoAddresses();

        customer.removeAddress(20L);

        assertThat(customer.getAddresses()).hasSize(1);
        assertThat(customer.getAddresses().get(0).getId()).isEqualTo(10L);
        assertThat(customer.getAddresses().get(0).isDefaultAddress()).isTrue();
    }

    @Test
    void shouldPromoteTheFirstRemainingAddressWhenRemovingTheDefault() {
        givenTwoAddresses();

        customer.removeAddress(10L);

        assertThat(customer.getAddresses()).hasSize(1);
        assertThat(customer.getAddresses().get(0).getId()).isEqualTo(20L);
        assertThat(customer.getAddresses().get(0).isDefaultAddress()).isTrue();
    }

    @Test
    void shouldNotRemoveTheLastAddress() {
        Address only = address(10L, "Rua A");
        only.setDefaultAddress(true);
        customer.setAddresses(new ArrayList<>(List.of(only)));

        assertThatThrownBy(() -> customer.removeAddress(10L))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(customer.getAddresses()).hasSize(1);
    }

    @Test
    void shouldThrowWhenRemovingAnUnknownAddress() {
        givenTwoAddresses();

        assertThatThrownBy(() -> customer.removeAddress(99L))
                .isInstanceOf(AddressNotFoundException.class);
    }
}