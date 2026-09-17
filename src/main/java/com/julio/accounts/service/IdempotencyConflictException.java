package com.julio.accounts.service;

public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException() {
        super("La clave ya fue utilizada con otros datos");
    }
}