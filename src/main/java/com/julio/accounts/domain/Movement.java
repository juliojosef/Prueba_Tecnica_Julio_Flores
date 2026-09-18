package com.julio.accounts.domain;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "movimiento",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_movimiento_cuenta_clave",
        columnNames = {"cuenta_id", "clave_idempotencia"}
    )
)
public class Movement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 10)
    private TransactionType type;

    @Column(
        name = "monto",
        nullable = false,
        precision = 19,
        scale = 2
    )
    private BigDecimal amount;

    @Column(
        name = "saldo_resultante",
        nullable = false,
        precision = 19,
        scale = 2
    )
    private BigDecimal resultingBalance;

    @Column(
        name = "clave_idempotencia",
        nullable = false,
        length = 100
    )
    private String idempotencyKey;

    @Column(name = "fecha", nullable = false)
    private Instant createdAt;

    protected Movement() {
        // Constructor requerido por JPA.
    }

    public Movement(
            Account account,
            TransactionType type,
            BigDecimal amount,
            String idempotencyKey) {

        this.account = account;
        this.type = type;
        this.amount = amount.setScale(
            2,
            RoundingMode.UNNECESSARY
        );
        this.resultingBalance = account.getBalance();
        this.idempotencyKey = idempotencyKey;
        this.createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    public Long getId() {
        return id;
    }

    public UUID getAccountId() {
        return account.getId();
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getResultingBalance() {
        return resultingBalance;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}