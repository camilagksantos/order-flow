package com.camilagksantos.orderflow.application.port.input;

import com.camilagksantos.orderflow.domain.customer.Customer;

public interface FindCustomerUseCase {

    Customer findById(Long id);
}
