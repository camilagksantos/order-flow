package com.camilagksantos.orderflow.application.port.input;

import com.camilagksantos.orderflow.domain.customer.Address;

public interface SetDefaultAddressUseCase {

    Address setDefaultAddress(Long customerId, Long addressId);
}