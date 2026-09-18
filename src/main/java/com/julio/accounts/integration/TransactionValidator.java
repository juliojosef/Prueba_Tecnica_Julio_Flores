package com.julio.accounts.integration;

import com.julio.accounts.dto.CreateTransactionRequest;

import java.util.UUID;

public interface TransactionValidator {

    void validate(
        UUID accountId,
        String key,
        CreateTransactionRequest request
    );
}