package com.julio.accounts.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;

import com.julio.accounts.domain.TransactionType;
import com.julio.accounts.dto.CreateTransactionRequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.math.BigDecimal;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HttpTransactionValidatorTest {

    private static final UUID ACCOUNT_ID =
        UUID.fromString("11111111-1111-1111-1111-111111111111");

    private WireMockServer server;
    private HttpTransactionValidator validator;

    @BeforeEach
    void startServer() {
        server = new WireMockServer(options().dynamicPort());
        server.start();

        validator = new HttpTransactionValidator(
            new ObjectMapper(),
            server.baseUrl(),
            1000
        );
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void shouldAcceptApprovedValidation() {
        server.stubFor(
            post(urlEqualTo("/validate"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"approved\":true}")
                )
        );

        assertDoesNotThrow(() -> validate());

        server.verify(
            1,
            postRequestedFor(urlEqualTo("/validate"))
                .withHeader(
                    "Idempotency-Key",
                    equalTo(ACCOUNT_ID + ":credit-1")
                )
                .withRequestBody(matchingJsonPath(
                    "$.accountId",
                    equalTo(ACCOUNT_ID.toString())
                ))
                .withRequestBody(matchingJsonPath(
                    "$.type",
                    equalTo("CREDIT")
                ))
        );
    }

    @Test
    void shouldRejectWithoutRetrying() {
        server.stubFor(
            post(urlEqualTo("/validate"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"approved\":false}")
                )
        );

        assertThrows(
            ValidationRejectedException.class,
            () -> validate()
        );

        server.verify(
            1,
            postRequestedFor(urlEqualTo("/validate"))
        );
    }

    @Test
    void shouldRetryServerFailureOnlyOnce() {
        server.stubFor(
            post(urlEqualTo("/validate"))
                .willReturn(aResponse().withStatus(503))
        );

        assertThrows(
            ValidationUnavailableException.class,
            () -> validate()
        );

        server.verify(
            2,
            postRequestedFor(urlEqualTo("/validate"))
        );
    }

    @Test
    void shouldRejectInvalidResponseWithoutRetrying() {
        server.stubFor(
            post(urlEqualTo("/validate"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"reason\":\"missing approved\"}")
                )
        );

        assertThrows(
            ValidationUnavailableException.class,
            () -> validate()
        );

        server.verify(
            1,
            postRequestedFor(urlEqualTo("/validate"))
        );
    }

    @Test
    @Timeout(10)
    void shouldStopWaitingForSlowService() {
        server.stubFor(
            post(urlEqualTo("/validate"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withFixedDelay(2000)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"approved\":true}")
                )
        );

        validator = new HttpTransactionValidator(
            new ObjectMapper(),
            server.baseUrl(),
            200
        );

        assertThrows(
            ValidationUnavailableException.class,
            () -> validate()
        );
    }

    @Test
    void shouldOpenCircuitAfterRepeatedFailures() {
        server.stubFor(
            post(urlEqualTo("/validate"))
                .willReturn(aResponse().withStatus(503))
        );

        // Dos validaciones con dos intentos cada una:
        // cuatro fallas abren el circuito.
        for (int i = 0; i < 2; i++) {
            assertThrows(
                ValidationUnavailableException.class,
                () -> validate()
            );
        }

        // El circuito abierto rechaza esta validacion
        // sin enviar otra solicitud HTTP.
        assertThrows(
            ValidationUnavailableException.class,
            () -> validate()
        );

        server.verify(
            4,
            postRequestedFor(urlEqualTo("/validate"))
        );
    }

    private void validate() {
        validator.validate(
            ACCOUNT_ID,
            "credit-1",
            new CreateTransactionRequest(
                TransactionType.CREDIT,
                new BigDecimal("50.00")
            )
        );
    }
}