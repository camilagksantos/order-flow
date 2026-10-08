package com.camilagksantos.orderflow.application.port.input;

import com.camilagksantos.orderflow.domain.customer.Address;

public interface AddAddressUseCase {

    Address addAddress(Long customerId, Address address);
}