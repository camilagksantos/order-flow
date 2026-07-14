package com.camilagksantos.orderflow.application.dto.response;

import com.camilagksantos.orderflow.domain.customer.CustomerStatus;

import java.util.List;

public record CustomerResponse(
        Long id,
        String name,
        String email,
        String nif,
        String phone,
        CustomerStatus status,
        List<AddressResponse> addresses
) {}