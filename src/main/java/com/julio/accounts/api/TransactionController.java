package com.julio.accounts.api;

import com.julio.accounts.dto.CreateTransactionRequest;
import com.julio.accounts.dto.TransactionResponse;
import com.julio.accounts.service.TransactionService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/accounts/{id}/transactions")
public class TransactionController {

    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> execute(
            @PathVariable("id") UUID id,
            @RequestHeader("Idempotency-Key") String key,
            @Valid @RequestBody CreateTransactionRequest request) {

        TransactionService.TransactionResult result =
            service.execute(id, key, request);

        HttpStatus status = result.replayed()
            ? HttpStatus.OK
            : HttpStatus.CREATED;

        return ResponseEntity
            .status(status)
            .header(
                "Idempotency-Replayed",
                Boolean.toString(result.replayed())
            )
            .body(result.response());
    }
}