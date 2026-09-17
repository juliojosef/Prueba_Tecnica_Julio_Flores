package com.julio.accounts.dto;

import com.julio.accounts.domain.Account;

import java.math.BigDecimal;
import java.util.UUID;

public record AccountResponse(
    UUID id,
    BigDecimal initialBalance,
    BigDecimal balance
) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(
            account.getId(),
            account.getInitialBalance(),
            account.getBalance()
        );
    }
}