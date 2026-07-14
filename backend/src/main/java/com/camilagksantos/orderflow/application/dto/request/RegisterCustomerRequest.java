package com.camilagksantos.orderflow.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterCustomerRequest(
        @NotBlank @Size(min = 2, max = 120) String name,
        @NotBlank @Email String email,
        @NotBlank @Pattern(regexp = "\\d{9}", message = "NIF must have exactly 9 digits") String nif,
        String phone
) {}