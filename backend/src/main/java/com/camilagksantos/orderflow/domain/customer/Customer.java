package com.camilagksantos.orderflow.domain.customer;

import com.camilagksantos.orderflow.domain.exception.AddressNotFoundException;
import com.camilagksantos.orderflow.domain.exception.BusinessRuleException;
import com.camilagksantos.orderflow.domain.shared.Email;
import com.camilagksantos.orderflow.domain.shared.NIF;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
    private Long id;
    private Long userId;
    private String name;
    private Email email;
    private NIF nif;
    private String phone;
    private CustomerStatus status;
    private List<Address> addresses;

    public void block() {
        this.status = CustomerStatus.BLOCKED;
    }

    public void activate() {
        this.status = CustomerStatus.ACTIVE;
    }

    public void addAddress(Address address) {
        List<Address> updated = new ArrayList<>(addresses == null ? List.of() : addresses);
        address.setCustomerId(id);
        address.setDefaultAddress(updated.isEmpty());
        updated.add(address);
        this.addresses = updated;
    }

    public Address findAddress(Long addressId) {
        List<Address> current = addresses == null ? List.of() : addresses;
        return current.stream()
                .filter(address -> addressId.equals(address.getId()))
                .findFirst()
                .orElseThrow(() -> new AddressNotFoundException(addressId));
    }

    public void updateAddress(Long addressId, Address data) {
        Address address = findAddress(addressId);
        address.setStreet(data.getStreet());
        address.setNumber(data.getNumber());
        address.setComplement(data.getComplement());
        address.setNeighborhood(data.getNeighborhood());
        address.setCity(data.getCity());
        address.setDistrict(data.getDistrict());
        address.setPostalCode(data.getPostalCode());
    }

    public void makeAddressDefault(Long addressId) {
        Address target = findAddress(addressId);
        addresses.forEach(address -> address.setDefaultAddress(address == target));
    }

    public void removeAddress(Long addressId) {
        Address target = findAddress(addressId);
        if (addresses.size() == 1) {
            throw new BusinessRuleException("A customer must keep at least one address");
        }
        List<Address> remaining = new ArrayList<>(addresses);
        remaining.remove(target);
        if (target.isDefaultAddress()) {
            remaining.get(0).setDefaultAddress(true);
        }
        this.addresses = remaining;
    }
}