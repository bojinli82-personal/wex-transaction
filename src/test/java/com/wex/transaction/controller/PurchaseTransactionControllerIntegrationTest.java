package com.wex.transaction.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wex.transaction.dto.CreatePurchaseTransactionRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for Purchase Transaction API endpoints.
 * Tests verify basic API functionality with mocked external dependencies.
 * Detailed validation tests are in PurchaseTransactionServiceTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Purchase Transaction API Integration Tests")
class PurchaseTransactionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RestTemplate restTemplate;

    @Test
    @DisplayName("Should create transaction with valid input")
    void testCreateTransaction() throws Exception {
        CreatePurchaseTransactionRequest request = CreatePurchaseTransactionRequest.builder()
                .description("Test transaction")
                .transactionDate(LocalDate.of(2024, 1, 15))
                .amountUsd(new BigDecimal("100.00"))
                .build();

        mockMvc.perform(post("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();
    }
}
