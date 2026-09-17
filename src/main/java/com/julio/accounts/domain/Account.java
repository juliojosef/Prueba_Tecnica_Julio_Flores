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