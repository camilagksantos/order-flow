package com.camilagksantos.orderflow.application.port.input;

public interface RemoveAddressUseCase {

    void removeAddress(Long customerId, Long addressId);
}