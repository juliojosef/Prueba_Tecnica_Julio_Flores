package com.julio.accounts.api;

import com.julio.accounts.domain.Account;
import com.julio.accounts.dto.AccountResponse;
import com.julio.accounts.dto.CreateAccountRequest;
import com.julio.accounts.service.AccountService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(
            @Valid @RequestBody CreateAccountRequest request) {

        Account account = service.create(request.initialBalance());

        URI location = URI.create("/accounts/" + account.getId());

        return ResponseEntity
            .created(location)
            .body(AccountResponse.from(account));
    }

    @GetMapping("/{id}")
    public AccountResponse findById(@PathVariable("id") UUID id) {
        Account account = service.findById(id);

        return AccountResponse.from(account);
    }
}