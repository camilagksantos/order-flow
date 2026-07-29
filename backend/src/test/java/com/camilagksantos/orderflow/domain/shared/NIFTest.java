package com.camilagksantos.orderflow.domain.shared;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class NIFTest {

    @Test
    void shouldCreateValidNIF() {
        NIF nif = new NIF("123456789");
        assertThat(nif.value()).isEqualTo("123456789");
    }

    @Test
    void shouldThrowWhenNIFIsBlank() {
        assertThatThrownBy(() -> new NIF(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowWhenNIFIsNull() {
        assertThatThrownBy(() -> new NIF(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowWhenNIFHasLetters() {
        assertThatThrownBy(() -> new NIF("12345678A"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowWhenNIFHasWrongLength() {
        assertThatThrownBy(() -> new NIF("12345678"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowWhenNIFIsInvalid() {
        assertThatThrownBy(() -> new NIF("111111111"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}