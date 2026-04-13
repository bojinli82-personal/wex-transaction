package com.wex.transaction.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wex.transaction.dto.CreatePurchaseTransactionRequest;
import com.wex.transaction.dto.CreatePurchaseTransactionResponse;
import com.wex.transaction.model.PurchaseTransaction;
import com.wex.transaction.repository.PurchaseTransactionRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Service layer for purchase transaction operations
 * 
 * This service handles business logic for:
 * - Creating and storing purchase transactions
 * - Retrieving transactions with currency conversion
 * - Currency conversion calculations
 * - Integration with Treasury API for exchange rates
 * 
 * Key features for Production deployment:
 * - Transactional integrity (all-or-nothing operations)
 * - Comprehensive error handling
 * - Detailed logging for debugging and monitoring
 * - Exception wrapping for clean error handling in controllers
 * 
 * @author Bo
 */
@Slf4j
@Service
public class PurchaseTransactionService {
    
    private final PurchaseTransactionRepository transactionRepository;

    /**
     * Constructor for dependency injection
     * 
     * @param transactionRepository Repository for database operations
     */
    public PurchaseTransactionService(PurchaseTransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Create and store a new purchase transaction
     * 
     * This method:
     * 1. Validates the input (performed by Spring's @Valid annotation at controller)
     * 2. Creates a new PurchaseTransaction entity
     * 3. Persists it to the database
     * 4. Returns the stored transaction with its auto-generated ID
     * 
     * Transaction behavior:
     * - Either the entire operation succeeds, or it fails with a rollback
     * - Prevents partial updates to the database
     * 
     * @param request The purchase transaction request containing description, date, and amount
     * @return Response containing the stored transaction and its unique ID
     * @throws IllegalArgumentException if any validation fails (shouldn't occur due to @Valid)
     */
    @Transactional
    public CreatePurchaseTransactionResponse createTransaction(CreatePurchaseTransactionRequest request) {
        log.debug("Creating new purchase transaction: description={}, date={}, amount={}",
                request.getDescription(), request.getTransactionDate(), request.getAmountUsd());

        try {
            // Build the entity from the request
            PurchaseTransaction transaction = PurchaseTransaction.builder()
                    .description(request.getDescription())
                    .transactionDate(request.getTransactionDate())
                    .amountUsd(request.getAmountUsd())
                    .build();

            // Persist to database (ID is auto-generated)
            PurchaseTransaction saved = transactionRepository.save(transaction);

            log.info("Purchase transaction created successfully. ID: {}, Amount: {} USD",
                    saved.getId(), saved.getAmountUsd());

            // Return response with the generated ID and confirmation
            return CreatePurchaseTransactionResponse.builder()
                    .id(saved.getId())
                    .description(saved.getDescription())
                    .transactionDate(saved.getTransactionDate())
                    .amountUsd(saved.getAmountUsd())
                    .message("Transaction created successfully")
                    .build();

        } catch (Exception e) {
            log.error("Failed to create purchase transaction: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create purchase transaction: " + e.getMessage(), e);
        }
    }
}
