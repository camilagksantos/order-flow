package com.camilagksantos.orderflow.application.dto.response;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {}