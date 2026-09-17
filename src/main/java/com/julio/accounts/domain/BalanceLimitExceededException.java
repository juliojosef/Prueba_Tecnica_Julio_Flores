package com.julio.accounts.domain;

public class BalanceLimitExceededException extends RuntimeException {

    public BalanceLimitExceededException() {
        super("El saldo resultante supera el monto permitido");
    }
}