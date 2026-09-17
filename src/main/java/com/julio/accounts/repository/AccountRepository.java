package com.julio.accounts.repository;

import com.julio.accounts.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccountRepository
        extends JpaRepository<Account, UUID> {
}