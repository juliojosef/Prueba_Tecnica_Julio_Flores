package com.julio.accounts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.julio.accounts.domain.Account;
import com.julio.accounts.repository.AccountRepository;
import com.julio.accounts.repository.MovementRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.julio.accounts.dto.CreateTransactionRequest;
import com.julio.accounts.integration.TransactionValidator;
import com.julio.accounts.integration.ValidationRejectedException;
import com.julio.accounts.integration.ValidationUnavailableException;

import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:movements_test;DB_CLOSE_DELAY=-1",
    "spring.h2.console.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class TransactionApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private MovementRepository movementRepository;

    @MockitoBean(enforceOverride = true)
    private TransactionValidator validator;

    @BeforeEach
    void cleanDatabase() {
        movementRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void creditShouldIncreaseBalanceAndSaveMovement()
            throws Exception {

        UUID id = createAccount("100.00");

        MvcResult result = send(
            id, "credit-1", "CREDIT", "50.00"
        );

        assertEquals(201, result.getResponse().getStatus());

        assertEquals(
            "false",
            result.getResponse().getHeader("Idempotency-Replayed")
        );

        assertBalance(id, "150.00");
        assertEquals(1L, movementRepository.count());
    }

    @Test
    void debitShouldDecreaseBalanceAndSaveMovement()
            throws Exception {

        UUID id = createAccount("100.00");

        MvcResult result = send(
            id, "debit-1", "DEBIT", "30.00"
        );

        assertEquals(201, result.getResponse().getStatus());
        assertBalance(id, "70.00");
        assertEquals(1L, movementRepository.count());
    }

    @Test
    void insufficientFundsShouldNotChangeDatabase()
            throws Exception {

        UUID id = createAccount("100.00");

        assertRejected(id, "debit-1", "DEBIT", "150.00", 409);

        assertBalance(id, "100.00");
        assertEquals(0L, movementRepository.count());
    }

    @Test
    void replayShouldReturnOriginalResponseWithoutChangingBalance()
            throws Exception {

        UUID id = createAccount("100.00");

        MvcResult original = send(
            id, "credit-1", "CREDIT", "50.00"
        );

        assertEquals(201, original.getResponse().getStatus());

        MvcResult repeated = send(
            id, "credit-1", "CREDIT", "50.00"
        );

        assertEquals(200, repeated.getResponse().getStatus());

        assertEquals(
            "true",
            repeated.getResponse().getHeader("Idempotency-Replayed")
        );

        assertSameBody(original, repeated);
        assertBalance(id, "150.00");
        assertEquals(1L, movementRepository.count());

        MvcResult debit = send(
            id, "debit-1", "DEBIT", "30.00"
        );

        assertEquals(201, debit.getResponse().getStatus());

        MvcResult repeatedAfterDebit = send(
            id, "credit-1", "CREDIT", "50.00"
        );

        assertEquals(
            200,
            repeatedAfterDebit.getResponse().getStatus()
        );

        assertSameBody(original, repeatedAfterDebit);
        assertBalance(id, "120.00");
        assertEquals(2L, movementRepository.count());
    }

    @Test
    void reusedKeyWithOtherDataShouldReturnConflict()
            throws Exception {

        UUID id = createAccount("100.00");

        MvcResult original = send(
            id, "credit-1", "CREDIT", "50.00"
        );

        assertEquals(201, original.getResponse().getStatus());

        assertRejected(
            id, "credit-1", "CREDIT", "60.00", 409
        );

        assertRejected(
            id, "credit-1", "DEBIT", "50.00", 409
        );

        assertBalance(id, "150.00");
        assertEquals(1L, movementRepository.count());
    }

    @Test
    void zeroAmountShouldBeRejected() throws Exception {
        UUID id = createAccount("100.00");

        assertRejected(id, "credit-1", "CREDIT", "0.00", 400);

        assertBalance(id, "100.00");
        assertEquals(0L, movementRepository.count());
    }

    @Test
    void negativeAmountShouldBeRejected() throws Exception {
        UUID id = createAccount("100.00");

        assertRejected(id, "credit-1", "CREDIT", "-5.00", 400);

        assertBalance(id, "100.00");
        assertEquals(0L, movementRepository.count());
    }

    @Test
    void moreThanTwoDecimalsShouldBeRejected()
            throws Exception {

        UUID id = createAccount("100.00");

        assertRejected(
            id, "credit-1", "CREDIT", "10.123", 400
        );

        assertBalance(id, "100.00");
        assertEquals(0L, movementRepository.count());
    }

    @Test
    void missingKeyShouldBeRejected() throws Exception {
        UUID id = createAccount("100.00");

        MvcResult result = mockMvc.perform(
            post("/accounts/" + id + "/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"CREDIT\",\"amount\":50.00}")
        ).andReturn();

        assertEquals(400, result.getResponse().getStatus());

        assertBalance(id, "100.00");
        assertEquals(0L, movementRepository.count());
    }

    @Test
    void unknownAccountShouldReturn404() throws Exception {
        UUID unknownId = new UUID(0L, 0L);

        assertRejected(
            unknownId, "credit-1", "CREDIT", "50.00", 404
        );

        assertEquals(0L, movementRepository.count());
    }

    @Test
    void simultaneousRequestsWithSameKeyShouldApplyOnce()
            throws Exception {

        UUID id = createAccount("100.00");

        List<MvcResult> results = runTogether(
            () -> send(id, "credit-1", "CREDIT", "50.00"),
            () -> send(id, "credit-1", "CREDIT", "50.00")
        );

        assertEquals(
            List.of(200, 201),
            sortedStatuses(results)
        );

        assertSameBody(results.get(0), results.get(1));
        assertBalance(id, "150.00");
        assertEquals(1L, movementRepository.count());
    }

    @Test
    void simultaneousDebitsShouldNotOverdrawAccount()
            throws Exception {

        UUID id = createAccount("100.00");

        List<MvcResult> results = runTogether(
            () -> send(id, "debit-1", "DEBIT", "80.00"),
            () -> send(id, "debit-2", "DEBIT", "80.00")
        );

        assertEquals(
            List.of(201, 409),
            sortedStatuses(results)
        );

        assertBalance(id, "20.00");
        assertEquals(1L, movementRepository.count());
    }
@Test
    void externalFailureShouldReturn503WithoutChanges()
            throws Exception {

        UUID id = createAccount("100.00");

        doThrow(new ValidationUnavailableException())
            .when(validator)
            .validate(
                any(UUID.class),
                anyString(),
                any(CreateTransactionRequest.class)
            );

        assertRejected(
            id, "credit-1", "CREDIT", "50.00", 503
        );

        assertBalance(id, "100.00");
        assertEquals(0L, movementRepository.count());
    }

    @Test
    void externalRejectionShouldReturn409WithoutChanges()
            throws Exception {

        UUID id = createAccount("100.00");

        doThrow(new ValidationRejectedException())
            .when(validator)
            .validate(
                any(UUID.class),
                anyString(),
                any(CreateTransactionRequest.class)
            );

        assertRejected(
            id, "credit-1", "CREDIT", "50.00", 409
        );

        assertBalance(id, "100.00");
        assertEquals(0L, movementRepository.count());
    }

    @Test
    void replayShouldSucceedWhenValidationIsDown()
            throws Exception {

        UUID id = createAccount("100.00");

        MvcResult original = send(
            id, "credit-1", "CREDIT", "50.00"
        );

        assertEquals(201, original.getResponse().getStatus());

        doThrow(new ValidationUnavailableException())
            .when(validator)
            .validate(
                any(UUID.class),
                anyString(),
                any(CreateTransactionRequest.class)
            );

        MvcResult repeated = send(
            id, "credit-1", "CREDIT", "50.00"
        );

        assertEquals(200, repeated.getResponse().getStatus());

        assertSameBody(original, repeated);
        assertBalance(id, "150.00");
        assertEquals(1L, movementRepository.count());

        verify(validator, times(1)).validate(
            any(UUID.class),
            anyString(),
            any(CreateTransactionRequest.class)
        );
    }

    private UUID createAccount(String initialBalance) {
        Account account = new Account(
            new BigDecimal(initialBalance)
        );

        return accountRepository.saveAndFlush(account).getId();
    }

    private MvcResult send(
            UUID id,
            String key,
            String type,
            String amount) throws Exception {

        String body =
            "{\"type\":\"" + type + "\",\"amount\":" + amount + "}";

        return mockMvc.perform(
            post("/accounts/" + id + "/transactions")
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andReturn();
    }

    private void assertRejected(
            UUID id,
            String key,
            String type,
            String amount,
            int expectedStatus) throws Exception {

        MvcResult result = send(id, key, type, amount);

        assertEquals(
            expectedStatus,
            result.getResponse().getStatus()
        );

        assertEquals(
            expectedStatus,
            objectMapper.readTree(
                result.getResponse().getContentAsString()
            ).get("status").asInt()
        );
    }

    private void assertBalance(UUID id, String expected) {
        Account account = accountRepository
            .findById(id)
            .orElseThrow();

        assertEquals(
            new BigDecimal(expected),
            account.getBalance()
        );
    }

    private void assertSameBody(
            MvcResult first,
            MvcResult second) throws Exception {

        assertEquals(
            objectMapper.readTree(
                first.getResponse().getContentAsString()
            ),
            objectMapper.readTree(
                second.getResponse().getContentAsString()
            )
        );
    }

    private List<Integer> sortedStatuses(
            List<MvcResult> results) {

        return results.stream()
            .map(result -> result.getResponse().getStatus())
            .sorted()
            .toList();
    }

    private List<MvcResult> runTogether(
            Callable<MvcResult> first,
            Callable<MvcResult> second) throws Exception {

        ExecutorService executor = Executors.newFixedThreadPool(2);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<MvcResult> firstResult = executor.submit(() -> {
                ready.countDown();

                assertTrue(
                    start.await(5, TimeUnit.SECONDS),
                    "No se libero la primera solicitud"
                );

                return first.call();
            });

            Future<MvcResult> secondResult = executor.submit(() -> {
                ready.countDown();

                assertTrue(
                    start.await(5, TimeUnit.SECONDS),
                    "No se libero la segunda solicitud"
                );

                return second.call();
            });

            assertTrue(
                ready.await(5, TimeUnit.SECONDS),
                "Las solicitudes no estuvieron listas a tiempo"
            );

            start.countDown();

            return List.of(
                firstResult.get(15, TimeUnit.SECONDS),
                secondResult.get(15, TimeUnit.SECONDS)
            );
        } finally {
            start.countDown();
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}