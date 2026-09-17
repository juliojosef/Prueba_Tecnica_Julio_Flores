package com.julio.accounts.service;

import com.julio.accounts.domain.Account;
import com.julio.accounts.repository.AccountRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class AccountService {

    private final AccountRepository repository;

    public AccountService(AccountRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Account create(BigDecimal initialBalance) {
        Account account = new Account(initialBalance);

        return repository.save(account);
    }

    @Transactional(readOnly = true)
    public Account findById(UUID id) {
        return repository.findById(id)
            .orElseThrow(() -> new AccountNotFoundException(id));
    }
}