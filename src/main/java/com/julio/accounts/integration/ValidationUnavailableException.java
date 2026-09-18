package com.julio.accounts.integration;

public class ValidationUnavailableException extends RuntimeException {

    public ValidationUnavailableException() {
        super("La validacion externa no esta disponible");
    }
}