package com.julio.accounts.api;

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
}