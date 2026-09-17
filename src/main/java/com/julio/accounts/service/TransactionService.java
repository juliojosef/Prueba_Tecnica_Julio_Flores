package com.julio.accounts.service;

import com.julio.accounts.domain.Account;
import com.julio.accounts.domain.Movement;
import com.julio.accounts.dto.CreateTransactionRequest;
import com.julio.accounts.dto.TransactionResponse;
import com.julio.accounts.repository.AccountRepository;
import com.julio.accounts.repository.MovementRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final MovementRepository movementRepository;

    public TransactionService(
            AccountRepository accountRepository,
            MovementRepository movementRepository) {

        this.accountRepository = accountRepository;
        this.movementRepository = movementRepository;
    }

    public record TransactionResult(
        TransactionResponse response,
        boolean replayed
    ) {
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
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

        Account account = accountRepository
            .findByIdForUpdate(accountId)
            .orElseThrow(
                () -> new AccountNotFoundException(accountId)
            );

        Optional<Movement> previous = movementRepository
            .findByAccount_IdAndIdempotencyKey(accountId, key);

        if (previous.isPresent()) {
            Movement movement = previous.get();

            boolean sameType =
                movement.getType() == request.type();

            boolean sameAmount =
                movement.getAmount()
                    .compareTo(request.amount()) == 0;

            if (!sameType || !sameAmount) {
                throw new IdempotencyConflictException();
            }

            return new TransactionResult(
                TransactionResponse.from(movement),
                true
            );
        }

        switch (request.type()) {
            case CREDIT -> account.credit(request.amount());
            case DEBIT -> account.debit(request.amount());
        }

        Movement movement = new Movement(
            account,
            request.type(),
            request.amount(),
            key
        );

        movementRepository.saveAndFlush(movement);

        return new TransactionResult(
            TransactionResponse.from(movement),
            false
        );
    }
}