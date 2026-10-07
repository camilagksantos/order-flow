package com.camilagksantos.orderflow.application.dto.response;

public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {}