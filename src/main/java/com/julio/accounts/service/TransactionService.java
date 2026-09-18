package com.julio.accounts.service;

import com.julio.accounts.dto.CreateTransactionRequest;
import com.julio.accounts.dto.TransactionResponse;
import com.julio.accounts.integration.TransactionValidator;

import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class TransactionService {

    private final TransactionWriter writer;
    private final TransactionValidator validator;

    public TransactionService(
            TransactionWriter writer,
            TransactionValidator validator) {

        this.writer = writer;
        this.validator = validator;
    }

    public record TransactionResult(
        TransactionResponse response,
        boolean replayed
    ) {
    }

    public TransactionResult execute(
            UUID accountId,
            String key,
            CreateTransactionRequest request) {

        if (key == null
                || !key.matches("[A-Za-z0-9._:-]{1,100}")) {

            throw new InvalidTransactionRequestException(
                "La clave debe tener entre 1 y 100 caracteres "
                    + "y usar letras, numeros, punto, guion, "
                    + "guion bajo o dos puntos"
            );
        }

        Optional<TransactionResult> previous =
            writer.findPrevious(accountId, key, request);

        if (previous.isPresent()) {
            return previous.get();
        }

        validator.validate(accountId, key, request);

        return writer.apply(accountId, key, request);
    }
}