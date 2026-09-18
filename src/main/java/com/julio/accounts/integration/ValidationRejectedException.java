package com.julio.accounts.integration;

public class ValidationRejectedException extends RuntimeException {

    public ValidationRejectedException() {
        super("El movimiento fue rechazado por la validacion externa");
    }
}