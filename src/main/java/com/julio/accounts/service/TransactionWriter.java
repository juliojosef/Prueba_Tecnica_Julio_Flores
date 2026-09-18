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
public class TransactionWriter {

    private final AccountRepository accountRepository;
    private final MovementRepository movementRepository;

    public TransactionWriter(
            AccountRepository accountRepository,
            MovementRepository movementRepository) {

        this.accountRepository = accountRepository;
        this.movementRepository = movementRepository;
    }

    @Transactional(readOnly = true)
    public Optional<TransactionService.TransactionResult> findPrevious(
            UUID accountId,
            String key,
            CreateTransactionRequest request) {

        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException(accountId);
        }

        return movementRepository
            .findByAccount_IdAndIdempotencyKey(accountId, key)
            .map(movement -> replay(movement, request));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionService.TransactionResult apply(
            UUID accountId,
            String key,
            CreateTransactionRequest request) {

        Account account = accountRepository
            .findByIdForUpdate(accountId)
            .orElseThrow(
                () -> new AccountNotFoundException(accountId)
            );

        Optional<Movement> previous = movementRepository
            .findByAccount_IdAndIdempotencyKey(accountId, key);

        if (previous.isPresent()) {
            return replay(previous.get(), request);
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

        return new TransactionService.TransactionResult(
            TransactionResponse.from(movement),
            false
        );
    }

    private TransactionService.TransactionResult replay(
            Movement movement,
            CreateTransactionRequest request) {

        boolean sameType =
            movement.getType() == request.type();

        boolean sameAmount =
            movement.getAmount().compareTo(request.amount()) == 0;

        if (!sameType || !sameAmount) {
            throw new IdempotencyConflictException();
        }

        return new TransactionService.TransactionResult(
            TransactionResponse.from(movement),
            true
        );
    }
}