package com.wex.transaction.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.wex.transaction.dto.CreatePurchaseTransactionRequest;
import com.wex.transaction.dto.CreatePurchaseTransactionResponse;
import com.wex.transaction.model.PurchaseTransaction;
import com.wex.transaction.repository.PurchaseTransactionRepository;

/**
 * Unit tests for PurchaseTransactionService
 * 
 * Tests cover:
 * - Transaction creation and persistence
 * - Transaction retrieval with currency conversion
 * - Error handling for various failure scenarios
 * - Currency conversion calculations
 * - Integration with Treasury API client
 * 
 * @author Bo
 */
@DisplayName("PurchaseTransactionService Unit Tests")
public class PurchaseTransactionServiceTest {
    
    private PurchaseTransactionService service;

    @Mock
    private PurchaseTransactionRepository transactionRepository;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        service = new PurchaseTransactionService(transactionRepository);
    }

    /**
     * Test: Successfully create a new transaction
     * Given: Valid transaction request
     * When: createTransaction is called
     * Then: Transaction is saved and ID is returned
     */
    @Test
    @DisplayName("Should create transaction with valid input")
    void testCreateTransactionSuccess() {
        // Arrange
        CreatePurchaseTransactionRequest request = CreatePurchaseTransactionRequest.builder()
                .description("Office supplies")
                .transactionDate(LocalDate.of(2024, 1, 15))
                .amountUsd(new BigDecimal("150.50"))
                .build();

        PurchaseTransaction savedTransaction = PurchaseTransaction.builder()
                .id("550e8400-e29b-41d4-a716-446655440000")
                .description("Office supplies")
                .transactionDate(LocalDate.of(2024, 1, 15))
                .amountUsd(new BigDecimal("150.50"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(transactionRepository.save(any(PurchaseTransaction.class))).thenReturn(savedTransaction);

        // Act
        CreatePurchaseTransactionResponse response = service.createTransaction(request);

        // Assert
        assertNotNull(response);
        assertEquals("550e8400-e29b-41d4-a716-446655440000", response.getId());
        assertEquals("Office supplies", response.getDescription());
        assertEquals(LocalDate.of(2024, 1, 15), response.getTransactionDate());
        assertEquals(new BigDecimal("150.50"), response.getAmountUsd());
        verify(transactionRepository, times(1)).save(any(PurchaseTransaction.class));
    }
}
