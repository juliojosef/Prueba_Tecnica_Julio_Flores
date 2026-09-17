package com.julio.accounts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.julio.accounts.repository.AccountRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:accounts_test;DB_CLOSE_DELAY=-1",
    "spring.h2.console.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class AccountApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void shouldCreateAndFindAccount() throws Exception {
        MvcResult result = mockMvc.perform(
                post("/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"initialBalance\":100.00}")
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.initialBalance").value(100.0))
            .andExpect(jsonPath("$.balance").value(100.0))
            .andReturn();

        String id = objectMapper
            .readTree(result.getResponse().getContentAsString())
            .get("id")
            .asText();

        assertEquals(
            "/accounts/" + id,
            result.getResponse().getHeader("Location")
        );

        mockMvc.perform(get("/accounts/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.initialBalance").value(100.0))
            .andExpect(jsonPath("$.balance").value(100.0));

        assertEquals(1L, repository.count());
    }

    @Test
    void shouldAcceptZeroInitialBalance() throws Exception {
        mockMvc.perform(
                post("/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"initialBalance\":0.00}")
            )
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.balance").value(0.0));

        assertEquals(1L, repository.count());
    }

    @Test
    void shouldRejectNegativeInitialBalance() throws Exception {
        mockMvc.perform(
                post("/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"initialBalance\":-5.00}")
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.errors.initialBalance").exists());

        assertEquals(0L, repository.count());
    }

    @Test
    void shouldRejectMissingInitialBalance() throws Exception {
        mockMvc.perform(
                post("/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}")
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.initialBalance").exists());

        assertEquals(0L, repository.count());
    }

    @Test
    void shouldRejectMoreThanTwoDecimals() throws Exception {
        mockMvc.perform(
                post("/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"initialBalance\":10.123}")
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.initialBalance").exists());

        assertEquals(0L, repository.count());
    }

    @Test
    void shouldReturn404ForUnknownAccount() throws Exception {
        mockMvc.perform(
                get("/accounts/00000000-0000-0000-0000-000000000000")
            )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldRejectInvalidIdentifier() throws Exception {
        mockMvc.perform(get("/accounts/abc"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void shouldRejectMalformedJson() throws Exception {
        mockMvc.perform(
                post("/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{")
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));

        assertEquals(0L, repository.count());
    }
}