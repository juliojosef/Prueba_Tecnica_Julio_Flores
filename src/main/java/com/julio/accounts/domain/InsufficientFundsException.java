package com.julio.accounts.domain;

public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException() {
        super("La cuenta no tiene fondos suficientes");
    }
}