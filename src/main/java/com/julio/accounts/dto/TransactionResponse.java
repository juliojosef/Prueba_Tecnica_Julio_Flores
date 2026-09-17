package com.julio.accounts.dto;

import com.julio.accounts.domain.Movement;
import com.julio.accounts.domain.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
    Long id,
    UUID accountId,
    TransactionType type,
    BigDecimal amount,
    BigDecimal resultingBalance,
    Instant createdAt
) {

    public static TransactionResponse from(Movement movement) {
        return new TransactionResponse(
            movement.getId(),
            movement.getAccountId(),
            movement.getType(),
            movement.getAmount(),
            movement.getResultingBalance(),
            movement.getCreatedAt()
        );
    }
}