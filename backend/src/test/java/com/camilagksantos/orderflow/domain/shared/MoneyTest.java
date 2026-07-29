package com.camilagksantos.orderflow.domain.shared;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

class MoneyTest {

    @Test
    void shouldCreateMoneyWithDefaultCurrency() {
        Money money = Money.of(BigDecimal.valueOf(10));
        assertThat(money.amount()).isEqualByComparingTo(BigDecimal.valueOf(10));
        assertThat(money.currency()).isEqualTo("EUR");
    }

    @Test
    void shouldAddTwoMoneyValues() {
        Money a = Money.of(BigDecimal.valueOf(10));
        Money b = Money.of(BigDecimal.valueOf(20));
        assertThat(a.add(b).amount()).isEqualByComparingTo(BigDecimal.valueOf(30));
    }

    @Test
    void shouldSubtractTwoMoneyValues() {
        Money a = Money.of(BigDecimal.valueOf(30));
        Money b = Money.of(BigDecimal.valueOf(10));
        assertThat(a.subtract(b).amount()).isEqualByComparingTo(BigDecimal.valueOf(20));
    }

    @Test
    void shouldMultiplyMoneyByQuantity() {
        Money money = Money.of(BigDecimal.valueOf(10));
        assertThat(money.multiply(3).amount()).isEqualByComparingTo(BigDecimal.valueOf(30));
    }

    @Test
    void shouldReturnZeroMoney() {
        Money zero = Money.zero();
        assertThat(zero.amount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldThrowWhenAmountIsNull() {
        assertThatThrownBy(() -> new Money(null, "EUR"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowWhenAmountIsNegative() {
        assertThatThrownBy(() -> Money.of(BigDecimal.valueOf(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldScaleToTwoDecimalPlaces() {
        Money money = Money.of(BigDecimal.valueOf(10.555));
        assertThat(money.amount().scale()).isEqualTo(2);
    }
}