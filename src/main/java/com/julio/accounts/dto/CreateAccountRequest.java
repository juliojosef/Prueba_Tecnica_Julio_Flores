package com.julio.accounts.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateAccountRequest(

    @NotNull(message = "El saldo inicial es obligatorio")
    @DecimalMin(
        value = "0.00",
        message = "El saldo inicial no puede ser negativo"
    )
    @Digits(
        integer = 17,
        fraction = 2,
        message = "El saldo permite hasta 17 enteros y 2 decimales"
    )
    BigDecimal initialBalance

) {
}