package com.camilagksantos.orderflow.application.port.input;

import com.camilagksantos.orderflow.domain.customer.Customer;

public interface RegisterCustomerUseCase {

    Customer execute(Customer customer);
}
