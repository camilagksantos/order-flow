package com.camilagksantos.orderflow.domain.shared;

public record NIF(
        String value
) {
    public NIF {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("NIF must not be blank");
        if (!value.matches("\\d{9}")) throw new IllegalArgumentException("NIF must have exactly 9 digits");
        if (!isValid(value)) throw new IllegalArgumentException("Invalid NIF");
    }

    private static boolean isValid(String nif) {
        int sum = 0;
        for (int i = 0; i < 8; i++) {
            sum += Character.getNumericValue(nif.charAt(i)) * (9 - i);
        }
        int remainder = sum % 11;
        int checkDigit = remainder < 2 ? 0 : 11 - remainder;
        return checkDigit == Character.getNumericValue(nif.charAt(8));
    }
}
