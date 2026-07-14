package com.camilagksantos.orderflow.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateAddressRequest(
        @NotBlank @Size(max = 200) String street,
        @NotBlank @Size(max = 20) String number,
        @Size(max = 100) String complement,
        @NotBlank @Size(max = 100) String neighborhood,
        @NotBlank @Size(max = 100) String city,
        @NotBlank @Size(max = 50) String district,
        @NotBlank @Pattern(regexp = "\\d{4}-\\d{3}", message = "Postal code must be in format XXXX-XXX") String postalCode
) {}