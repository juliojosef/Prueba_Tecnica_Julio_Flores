package com.julio.accounts.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.julio.accounts.dto.CreateTransactionRequest;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

@Component
public class HttpTransactionValidator
        implements TransactionValidator {

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final URI endpoint;
    private final long timeoutMillis;

    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public HttpTransactionValidator(
            ObjectMapper objectMapper,
            @Value("${validation.base-url}") String baseUrl,
            @Value("${validation.timeout-ms}") long timeoutMillis) {

        if (timeoutMillis <= 0) {
            throw new IllegalArgumentException(
                "El timeout debe ser mayor que cero"
            );
        }

        this.objectMapper = objectMapper;
        this.endpoint = URI.create(
            baseUrl.replaceAll("/+$", "") + "/validate"
        );
        this.timeoutMillis = timeoutMillis;

        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(
                Math.min(300L, timeoutMillis)
            ))
            .build();

        CircuitBreakerConfig breakerConfig =
            CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(4)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(2)
                .recordExceptions(
                    ValidationUnavailableException.class
                )
                .build();

        this.circuitBreaker = CircuitBreaker.of(
            "externalValidation",
            breakerConfig
        );

        RetryConfig retryConfig = RetryConfig.custom()
            .maxAttempts(2)
            .waitDuration(Duration.ofMillis(150))
            .retryExceptions(RetryableValidationException.class)
            .ignoreExceptions(CallNotPermittedException.class)
            .build();

        this.retry = Retry.of(
            "externalValidation",
            retryConfig
        );
    }

    @Override
    public void validate(
            UUID accountId,
            String key,
            CreateTransactionRequest request) {

        Supplier<Boolean> guardedCall =
            CircuitBreaker.decorateSupplier(
                circuitBreaker,
                () -> callRemote(accountId, key, request)
            );

        Supplier<Boolean> retriedCall =
            Retry.decorateSupplier(retry, guardedCall);

        boolean approved;

        try {
            approved = retriedCall.get();
        } catch (CallNotPermittedException
                | ValidationUnavailableException exception) {

            // Fallback: detener la operacion con un error controlado.
            throw new ValidationUnavailableException();
        }

        if (!approved) {
            throw new ValidationRejectedException();
        }
    }

    private boolean callRemote(
            UUID accountId,
            String key,
            CreateTransactionRequest request) {

        try {
            ObjectNode body = objectMapper.createObjectNode();

            body.put("accountId", accountId.toString());
            body.put("type", request.type().name());
            body.put("amount", request.amount());

            HttpRequest httpRequest = HttpRequest
                .newBuilder(endpoint)
                .timeout(Duration.ofMillis(timeoutMillis))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header(
                    "Idempotency-Key",
                    accountId + ":" + key
                )
                .POST(HttpRequest.BodyPublishers.ofString(
                    objectMapper.writeValueAsString(body)
                ))
                .build();

            CompletableFuture<HttpResponse<String>> pending =
                httpClient.sendAsync(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString()
                );

            HttpResponse<String> response =
                awaitResponse(pending);

            int status = response.statusCode();

            if (status == 429 || status >= 500 && status <= 599) {
                throw new RetryableValidationException();
            }

            if (status != 200) {
                throw new ValidationUnavailableException();
            }

            JsonNode responseBody =
                objectMapper.readTree(response.body());

            if (responseBody == null
                    || !responseBody.path("approved").isBoolean()) {

                throw new ValidationUnavailableException();
            }

            return responseBody.get("approved").booleanValue();

        } catch (JsonProcessingException exception) {
            throw new ValidationUnavailableException();
        }
    }

    private HttpResponse<String> awaitResponse(
            CompletableFuture<HttpResponse<String>> pending) {

        try {
            return pending.get(
                timeoutMillis,
                TimeUnit.MILLISECONDS
            );

        } catch (TimeoutException exception) {
            pending.cancel(true);
            throw new RetryableValidationException();

        } catch (InterruptedException exception) {
            pending.cancel(true);
            Thread.currentThread().interrupt();
            throw new ValidationUnavailableException();

        } catch (ExecutionException exception) {
            if (exception.getCause() instanceof IOException) {
                throw new RetryableValidationException();
            }

            throw new ValidationUnavailableException();
        }
    }

    private static class RetryableValidationException
            extends ValidationUnavailableException {
    }
}