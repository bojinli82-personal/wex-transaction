package com.wex.transaction.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wex.transaction.dto.CreatePurchaseTransactionRequest;
import org.junit.jupiter.api.BeforeEach;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @BeforeEach
    void setUp() {
        // Reset mock before each test
        org.mockito.Mockito.reset(restTemplate);
    }

    @Test
    @DisplayName("Should create transaction with valid input and return 201 with full response")
    void testCreateTransactionSuccess() throws Exception {
        CreatePurchaseTransactionRequest request = CreatePurchaseTransactionRequest.builder()
                .description("Office supplies")
                .transactionDate(LocalDate.of(2024, 1, 15))
                .amountUsd(new BigDecimal("100.00"))
                .build();

        mockMvc.perform(post("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.description").value("Office supplies"))
                .andExpect(jsonPath("$.transactionDate").value("2024-01-15"))
                .andExpect(jsonPath("$.amountUsd").value(100.00))
                .andExpect(jsonPath("$.message").value("Transaction created successfully"))
                .andReturn();
    }

    @Test
    @DisplayName("Should return 404 for non-existent transaction with proper error structure")
    void testGetTransactionNotFound() throws Exception {
        mockMvc.perform(get("/transactions/non-existent-id")
                .param("currency", "GBP"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").exists())
                .andReturn();
    }

    @Test
    @DisplayName("Should return 422 when currency conversion fails due to unavailable rate")
    void testGetTransactionCurrencyConversionError() throws Exception {
        // First, create a transaction
        CreatePurchaseTransactionRequest createRequest = CreatePurchaseTransactionRequest.builder()
                .description("Test purchase")
                .transactionDate(LocalDate.of(2024, 1, 15))
                .amountUsd(new BigDecimal("100.00"))
                .build();

        String createResponse = mockMvc.perform(post("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract transaction ID
        String transactionId = objectMapper.readTree(createResponse).get("id").asText();

        // Try to retrieve with an invalid currency code - should fail conversion
        mockMvc.perform(get("/transactions/{id}", transactionId)
                .param("currency", "XXX"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").exists())
                .andReturn();
    }

    // ============ VALIDATION ERROR TESTS ============

    @Test
    @DisplayName("Should return 400 when description is missing")
    void testCreateTransactionMissingDescription() throws Exception {
        String requestJson = "{\"transactionDate\":\"2024-01-15\",\"amountUsd\":100.00}";

        mockMvc.perform(post("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    @DisplayName("Should return 400 when description is blank")
    void testCreateTransactionBlankDescription() throws Exception {
        CreatePurchaseTransactionRequest request = CreatePurchaseTransactionRequest.builder()
                .description("   ")
                .transactionDate(LocalDate.of(2024, 1, 15))
                .amountUsd(new BigDecimal("100.00"))
                .build();

        mockMvc.perform(post("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    @DisplayName("Should return 400 when description exceeds 50 characters")
    void testCreateTransactionDescriptionTooLong() throws Exception {
        CreatePurchaseTransactionRequest request = CreatePurchaseTransactionRequest.builder()
                .description("This is a very long description that exceeds the maximum allowed fifty character limit")
                .transactionDate(LocalDate.of(2024, 1, 15))
                .amountUsd(new BigDecimal("100.00"))
                .build();

        mockMvc.perform(post("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    @DisplayName("Should return 400 when transaction date is missing")
    void testCreateTransactionMissingDate() throws Exception {
        String requestJson = "{\"description\":\"Office supplies\",\"amountUsd\":100.00}";

        mockMvc.perform(post("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    @DisplayName("Should return 400 when transaction date is in the future")
    void testCreateTransactionFutureDate() throws Exception {
        LocalDate futureDate = LocalDate.now().plusDays(1);
        CreatePurchaseTransactionRequest request = CreatePurchaseTransactionRequest.builder()
                .description("Office supplies")
                .transactionDate(futureDate)
                .amountUsd(new BigDecimal("100.00"))
                .build();

        mockMvc.perform(post("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    @DisplayName("Should return 400 when amount is missing")
    void testCreateTransactionMissingAmount() throws Exception {
        String requestJson = "{\"description\":\"Office supplies\",\"transactionDate\":\"2024-01-15\"}";

        mockMvc.perform(post("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    @DisplayName("Should return 400 when amount is negative")
    void testCreateTransactionNegativeAmount() throws Exception {
        CreatePurchaseTransactionRequest request = CreatePurchaseTransactionRequest.builder()
                .description("Office supplies")
                .transactionDate(LocalDate.of(2024, 1, 15))
                .amountUsd(new BigDecimal("-100.00"))
                .build();

        mockMvc.perform(post("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    @DisplayName("Should return 400 when amount is zero")
    void testCreateTransactionZeroAmount() throws Exception {
        CreatePurchaseTransactionRequest request = CreatePurchaseTransactionRequest.builder()
                .description("Office supplies")
                .transactionDate(LocalDate.of(2024, 1, 15))
                .amountUsd(BigDecimal.ZERO)
                .build();

        mockMvc.perform(post("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    @DisplayName("Should return 422 when currency parameter value causes conversion to fail")
    void testGetTransactionConversionFailure() throws Exception {
        // First, create a transaction
        CreatePurchaseTransactionRequest createRequest = CreatePurchaseTransactionRequest.builder()
                .description("Test purchase")
                .transactionDate(LocalDate.of(2024, 1, 15))
                .amountUsd(new BigDecimal("100.00"))
                .build();

        String createResponse = mockMvc.perform(post("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract transaction ID
        String transactionId = objectMapper.readTree(createResponse).get("id").asText();

        // Try to retrieve with an invalid currency code - should trigger conversion failure
        mockMvc.perform(get("/transactions/{id}", transactionId)
                .param("currency", "XXX"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andReturn();
    }

    @Test
    @DisplayName("Should return 400 when request body is invalid JSON")
    void testCreateTransactionInvalidJson() throws Exception {
        String invalidJson = "{\"description\":\"Office\"}invalid}";

        mockMvc.perform(post("/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andReturn();
    }
}
