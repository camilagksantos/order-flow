package com.camilagksantos.orderflow.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCategoryRequest(
        @NotBlank @Size(min = 2, max = 80) String name
) {}