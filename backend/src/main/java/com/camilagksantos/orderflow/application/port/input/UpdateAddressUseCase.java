package com.camilagksantos.orderflow.application.port.input;

import com.camilagksantos.orderflow.domain.customer.Address;

public interface UpdateAddressUseCase {

    Address updateAddress(Long customerId, Long addressId, Address data);
}