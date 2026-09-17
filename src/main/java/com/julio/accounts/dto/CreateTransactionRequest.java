package com.julio.accounts.dto;

import com.julio.accounts.domain.TransactionType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateTransactionRequest(

    @NotNull(message = "El tipo es obligatorio")
    TransactionType type,

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(
        value = "0.01",
        message = "El monto debe ser mayor que cero"
    )
    @Digits(
        integer = 17,
        fraction = 2,
        message = "El monto permite hasta 17 enteros y 2 decimales"
    )
    BigDecimal amount

) {
}