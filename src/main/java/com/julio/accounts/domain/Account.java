package com.julio.accounts.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Entity
@Table(name = "cuenta")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(
        name = "saldo_inicial",
        nullable = false,
        updatable = false,
        precision = 19,
        scale = 2
    )
    private BigDecimal initialBalance;

    @Column(
        name = "saldo",
        nullable = false,
        precision = 19,
        scale = 2
    )
    private BigDecimal balance;

    @Version
    private Long version;

    protected Account() {
        // Constructor requerido por JPA.
    }

    public Account(BigDecimal initialBalance) {
        if (initialBalance == null || initialBalance.signum() < 0) {
            throw new IllegalArgumentException(
                "El saldo inicial debe ser mayor o igual a cero"
            );
        }

        BigDecimal amount =
            initialBalance.setScale(2, RoundingMode.UNNECESSARY);

        if (amount.precision() > 19) {
            throw new IllegalArgumentException(
                "El saldo inicial supera el monto permitido"
            );
        }

        this.initialBalance = amount;
        this.balance = amount;
    }

    public void credit(BigDecimal amount) {
        BigDecimal validAmount = normalizePositiveAmount(amount);

        BigDecimal newBalance = balance.add(validAmount);

        if (newBalance.precision() > 19) {
            throw new BalanceLimitExceededException();
        }

        balance = newBalance;
    }

    public void debit(BigDecimal amount) {
        BigDecimal validAmount = normalizePositiveAmount(amount);

        if (balance.compareTo(validAmount) < 0) {
            throw new InsufficientFundsException();
        }

        balance = balance.subtract(validAmount);
    }

    private static BigDecimal normalizePositiveAmount(
            BigDecimal amount) {

        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException(
                "El monto debe ser mayor que cero"
            );
        }

        BigDecimal normalized;

        try {
            normalized = amount.setScale(
                2,
                RoundingMode.UNNECESSARY
            );
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                "El monto debe poder representarse con dos decimales",
                exception
            );
        }

        if (normalized.precision() > 19) {
            throw new IllegalArgumentException(
                "El monto supera el limite permitido"
            );
        }

        return normalized;
    }

    public UUID getId() {
        return id;
    }

    public BigDecimal getInitialBalance() {
        return initialBalance;
    }

    public BigDecimal getBalance() {
        return balance;
    }
}