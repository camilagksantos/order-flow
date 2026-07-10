package com.camilagksantos.orderflow.domain.customer;

import com.camilagksantos.orderflow.domain.shared.Email;
import com.camilagksantos.orderflow.domain.shared.NIF;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
}