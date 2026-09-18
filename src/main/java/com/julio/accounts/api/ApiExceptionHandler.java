package com.julio.accounts.api;

import com.julio.accounts.domain.InsufficientFundsException;
import com.julio.accounts.domain.BalanceLimitExceededException;
import com.julio.accounts.service.IdempotencyConflictException;
import com.julio.accounts.service.InvalidTransactionRequestException;
import org.springframework.web.bind.MissingRequestHeaderException;

import com.julio.accounts.service.AccountNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

import com.julio.accounts.integration.ValidationUnavailableException;
import com.julio.accounts.integration.ValidationRejectedException;

@RestControllerAdvice
public class ApiExceptionHandler {

    public record ApiError(
        int status,
        String message,
        Map<String, String> errors
    ) {
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ApiError> accountNotFound(
            AccountNotFoundException exception) {

        ApiError error = new ApiError(
            404,
            exception.getMessage(),
            Map.of()
        );

        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(
            MethodArgumentNotValidException exception) {

        Map<String, String> errors = new LinkedHashMap<>();

        exception.getBindingResult()
            .getFieldErrors()
            .forEach(error -> errors.put(
                error.getField(),
                error.getDefaultMessage()
            ));

        return ResponseEntity.badRequest().body(
            new ApiError(400, "Datos invalidos", errors)
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> invalidBody() {
        return ResponseEntity.badRequest().body(
            new ApiError(
                400,
                "El cuerpo debe contener un JSON valido con tipos correctos",
                Map.of()
            )
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> invalidIdentifier() {
        return ResponseEntity.badRequest().body(
            new ApiError(
                400,
                "El identificador debe tener formato UUID",
                Map.of()
            )
        );
    }

    @ExceptionHandler({
        InsufficientFundsException.class,
        BalanceLimitExceededException.class,
        IdempotencyConflictException.class
    })
    public ResponseEntity<ApiError> businessConflict(
            RuntimeException exception) {

        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(new ApiError(
                409,
                exception.getMessage(),
                Map.of()
            ));
    }

    @ExceptionHandler(InvalidTransactionRequestException.class)
    public ResponseEntity<ApiError> invalidTransaction(
            InvalidTransactionRequestException exception) {

        return ResponseEntity.badRequest().body(
            new ApiError(
                400,
                exception.getMessage(),
                Map.of()
            )
        );
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiError> missingHeader() {
        return ResponseEntity.badRequest().body(
            new ApiError(
                400,
                "Falta una cabecera obligatoria; "
                    + "para movimientos envia Idempotency-Key",
                Map.of()
            )
        );
    }
    @ExceptionHandler(ValidationUnavailableException.class)
    public ResponseEntity<ApiError> validationUnavailable() {
        return ResponseEntity
            .status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new ApiError(
                503,
                "La validacion externa no esta disponible; "
                    + "reintenta usando la misma clave",
                Map.of()
            ));
    }

    @ExceptionHandler(ValidationRejectedException.class)
    public ResponseEntity<ApiError> validationRejected() {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(new ApiError(
                409,
                "El movimiento fue rechazado por "
                    + "la validacion externa",
                Map.of()
            ));
    }
}