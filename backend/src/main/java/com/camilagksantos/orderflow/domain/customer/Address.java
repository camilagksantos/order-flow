package com.camilagksantos.orderflow.domain.customer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    private Long id;
    private Long customerId;
    private String street;
    private String number;
    private String complement;
    private String neighborhood;
    private String city;
    private String district;
    private String postalCode;
    private String country;
    private boolean isDefault;
}