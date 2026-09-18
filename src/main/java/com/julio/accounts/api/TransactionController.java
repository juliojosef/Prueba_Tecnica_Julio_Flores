package com.julio.accounts.api;

import com.julio.accounts.dto.CreateTransactionRequest;
import com.julio.accounts.dto.TransactionResponse;
import com.julio.accounts.service.TransactionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/accounts/{id}/transactions")
@Tag(name = "Movimientos")
public class TransactionController {

    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(
        summary = "Registrar un credito o debito",
        description =
            "La clave es unica por cuenta. Repetir la misma clave "
            + "con el mismo tipo y monto devuelve el movimiento "
            + "original sin volver a modificar el saldo."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Movimiento creado",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TransactionResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "200",
            description = "Solicitud repetida; devuelve el movimiento original",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = TransactionResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos invalidos o clave ausente o invalida",
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
        ),
        @ApiResponse(
            responseCode = "409",
            description =
                "Fondos insuficientes, limite de saldo "
                + "o clave utilizada con otros datos",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(
                    implementation = ApiExceptionHandler.ApiError.class
                )
            )
        )
    })
    public ResponseEntity<TransactionResponse> execute(
            @PathVariable("id") UUID id,

            @Parameter(
                description =
                    "Clave de 1 a 100 caracteres. "
                    + "Permite letras, numeros, punto, guion, "
                    + "guion bajo y dos puntos.",
                example = "credito-001"
            )
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