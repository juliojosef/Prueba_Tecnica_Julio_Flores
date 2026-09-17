package com.julio.accounts.service;

public class InvalidTransactionRequestException
        extends RuntimeException {

    public InvalidTransactionRequestException(String message) {
        super(message);
    }
}