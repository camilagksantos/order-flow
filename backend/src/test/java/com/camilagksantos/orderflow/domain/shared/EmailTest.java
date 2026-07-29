package com.camilagksantos.orderflow.domain.shared;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class EmailTest {

    @Test
    void shouldCreateValidEmail() {
        Email email = new Email("camila@test.com");
        assertThat(email.value()).isEqualTo("camila@test.com");
    }

    @Test
    void shouldConvertToLowercase() {
        Email email = new Email("CAMILA@TEST.COM");
        assertThat(email.value()).isEqualTo("camila@test.com");
    }

    @Test
    void shouldThrowWhenEmailIsBlank() {
        assertThatThrownBy(() -> new Email(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowWhenEmailIsNull() {
        assertThatThrownBy(() -> new Email(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowWhenEmailFormatIsInvalid() {
        assertThatThrownBy(() -> new Email("invalid-email"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowWhenEmailHasNoAt() {
        assertThatThrownBy(() -> new Email("camilatest.com"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}