package com.julio.accounts.api;

import com.julio.accounts.domain.Account;
import com.julio.accounts.dto.AccountResponse;
import com.julio.accounts.dto.CreateAccountRequest;
import com.julio.accounts.service.AccountService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/accounts")
@Tag(name = "Cuentas", description = "Creacion y consulta de cuentas")
public class AccountController {

    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Crear una cuenta con saldo inicial")
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Cuenta creada",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AccountResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Saldo inicial o JSON invalido",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    implementation = ApiExceptionHandler.ApiError.class
                )
            )
        )
    })
    public ResponseEntity<AccountResponse> create(
            @Valid @RequestBody CreateAccountRequest request) {

        Account account = service.create(request.initialBalance());

        URI location = URI.create("/accounts/" + account.getId());

        return ResponseEntity
            .created(location)
            .body(AccountResponse.from(account));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar una cuenta y su saldo")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Cuenta encontrada",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AccountResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Identificador invalido",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    implementation = ApiExceptionHandler.ApiError.class
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Cuenta inexistente",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    implementation = ApiExceptionHandler.ApiError.class
                )
            )
        )
    })
    public AccountResponse findById(@PathVariable("id") UUID id) {
        Account account = service.findById(id);

        return AccountResponse.from(account);
    }
}