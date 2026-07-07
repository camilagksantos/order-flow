package com.camilagksantos.orderflow.domain.customer;

public record Address(
        Long id,
        Long customerId,
        String street,
        String number,
        String complement,
        String neighborhood,
        String city,
        String district,
        String postalCode,
        String country,
        boolean isDefault
) {
    public Address {
        if (street == null || street.isBlank()) throw new IllegalArgumentException("Street must not be blank");
        if (number == null || number.isBlank()) throw new IllegalArgumentException("Number must not be blank");
        if (neighborhood == null || neighborhood.isBlank()) throw new IllegalArgumentException("Neighborhood must not be blank");
        if (city == null || city.isBlank()) throw new IllegalArgumentException("City must not be blank");
        if (district == null || district.isBlank()) throw new IllegalArgumentException("District must not be blank");
        if (postalCode == null || postalCode.isBlank()) throw new IllegalArgumentException("Postal code must not be blank");
        country = country == null ? "PT" : country;
    }
}
