package com.julio.accounts.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccountTest {

    @Test
    void creditShouldIncreaseBalance() {
        Account account = new Account(new BigDecimal("100.00"));

        account.credit(new BigDecimal("50.00"));

        assertEquals(
            new BigDecimal("150.00"),
            account.getBalance()
        );

        assertEquals(
            new BigDecimal("100.00"),
            account.getInitialBalance()
        );
    }

    @Test
    void debitShouldDecreaseBalance() {
        Account account = new Account(new BigDecimal("100.00"));

        account.debit(new BigDecimal("30.00"));

        assertEquals(
            new BigDecimal("70.00"),
            account.getBalance()
        );

        assertEquals(
            new BigDecimal("100.00"),
            account.getInitialBalance()
        );
    }

    @Test
    void debitShouldAllowUsingEntireBalance() {
        Account account = new Account(new BigDecimal("100.00"));

        account.debit(new BigDecimal("100.00"));

        assertEquals(
            new BigDecimal("0.00"),
            account.getBalance()
        );
    }

    @Test
    void insufficientFundsShouldPreserveBalance() {
        Account account = new Account(new BigDecimal("100.00"));

        assertThrows(
            InsufficientFundsException.class,
            () -> account.debit(new BigDecimal("150.00"))
        );

        assertEquals(
            new BigDecimal("100.00"),
            account.getBalance()
        );
    }

    @Test
    void operationsShouldRejectZeroAmount() {
        Account account = new Account(new BigDecimal("100.00"));

        assertThrows(
            IllegalArgumentException.class,
            () -> account.credit(BigDecimal.ZERO)
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> account.debit(BigDecimal.ZERO)
        );

        assertEquals(
            new BigDecimal("100.00"),
            account.getBalance()
        );
    }

    @Test
    void operationsShouldRejectNegativeAmount() {
        Account account = new Account(new BigDecimal("100.00"));

        assertThrows(
            IllegalArgumentException.class,
            () -> account.credit(new BigDecimal("-5.00"))
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> account.debit(new BigDecimal("-5.00"))
        );

        assertEquals(
            new BigDecimal("100.00"),
            account.getBalance()
        );
    }

    @Test
    void operationsShouldRejectAmountRequiringRounding() {
        Account account = new Account(new BigDecimal("100.00"));

        assertThrows(
            IllegalArgumentException.class,
            () -> account.credit(new BigDecimal("10.123"))
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> account.debit(new BigDecimal("10.123"))
        );

        assertEquals(
            new BigDecimal("100.00"),
            account.getBalance()
        );
    }

    @Test
    void excessiveCreditShouldPreserveBalance() {
        BigDecimal maximum =
            new BigDecimal("99999999999999999.99");

        Account account = new Account(maximum);

        assertThrows(
            BalanceLimitExceededException.class,
            () -> account.credit(new BigDecimal("0.01"))
        );

        assertEquals(maximum, account.getBalance());
    }
}