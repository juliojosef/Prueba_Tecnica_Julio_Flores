package com.julio.accounts.service;

import java.util.UUID;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(UUID id) {
        super("No existe la cuenta " + id);
    }
}