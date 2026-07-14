package com.camilagksantos.orderflow.application.dto.response;

public record AddressResponse(
        Long id,
        String street,
        String number,
        String complement,
        String neighborhood,
        String city,
        String district,
        String postalCode,
        String country,
        boolean isDefault
) {}