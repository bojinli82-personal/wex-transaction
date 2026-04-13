package com.wex.transaction.service;

import com.wex.transaction.exception.CurrencyConversionException;
import com.wex.transaction.exception.TransactionNotFoundException;
import com.wex.transaction.external.TreasuryExchangeRateClient;
import com.wex.transaction.model.ExchangeRate;
import com.wex.transaction.model.PurchaseTransaction;
import com.wex.transaction.dto.CreatePurchaseTransactionRequest;
import com.wex.transaction.dto.CreatePurchaseTransactionResponse;
import com.wex.transaction.dto.PurchaseTransactionResponse;
import com.wex.transaction.repository.PurchaseTransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
    private final TreasuryExchangeRateClient exchangeRateClient;

    /**
     * Constructor for dependency injection
     * 
     * @param transactionRepository Repository for database operations
     * @param exchangeRateClient Client for Treasury API calls
     */
    public PurchaseTransactionService(PurchaseTransactionRepository transactionRepository,
                                      TreasuryExchangeRateClient exchangeRateClient) {
        this.transactionRepository = transactionRepository;
        this.exchangeRateClient = exchangeRateClient;
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

    /**
     * Retrieve a purchase transaction and convert it to a specified currency
     * 
     * This method:
     * 1. Finds the transaction by ID
     * 2. Fetches the exchange rate from Treasury API for the transaction date
     * 3. Applies the conversion formula: amountUsd * exchangeRate
     * 4. Rounds the result to 2 decimal places
     * 5. Returns complete transaction details with conversion information
     * 
     * Error handling:
     * - Throws TransactionNotFoundException if ID doesn't exist
     * - Throws CurrencyConversionException if exchange rate cannot be found
     * 
     * @param transactionId The unique identifier of the transaction
     * @param currencyCode The target currency code (ISO 4217, e.g., "GBP", "CAD")
     * @return Transaction response with original amount and converted amount
     * @throws TransactionNotFoundException if transaction doesn't exist
     * @throws CurrencyConversionException if currency conversion fails
     */
    @Transactional(readOnly = true)
    public PurchaseTransactionResponse getTransactionWithConversion(String transactionId, String currencyCode) {
        // Validate currency code format early
        if (currencyCode == null || currencyCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Currency code cannot be null or empty");
        }
        currencyCode = currencyCode.trim().toUpperCase();
        if (!currencyCode.matches("^[A-Z]{3}$")) {
            throw new IllegalArgumentException(
                    String.format("Invalid currency code format: %s. Must be 3-letter ISO 4217 code", currencyCode));
        }

        log.debug("Retrieving transaction {} and converting to {}", transactionId, currencyCode);

        try {
            // Fetch the transaction from database
            PurchaseTransaction transaction = transactionRepository.findById(transactionId)
                    .orElseThrow(() -> {
                        log.warn("Transaction not found: {}", transactionId);
                        return new TransactionNotFoundException(
                                String.format("Purchase transaction with ID %s not found", transactionId));
                    });

            // Fetch exchange rate from Treasury API
            ExchangeRate exchangeRate;
            try {
                exchangeRate = exchangeRateClient.getExchangeRate(currencyCode, transaction.getTransactionDate());
                
                // Validate exchangeRate is not null and has valid values
                if (exchangeRate == null) {
                    throw new CurrencyConversionException(
                            String.format("Treasury API returned null exchange rate for %s", currencyCode));
                }
                if (exchangeRate.getExchangeRate() == null 
                        || exchangeRate.getExchangeRate().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new CurrencyConversionException(
                            String.format("Invalid exchange rate received for %s: %s", 
                                    currencyCode, exchangeRate.getExchangeRate()));
                }
            } catch (CurrencyConversionException e) {
                // Re-throw business logic exceptions
                throw e;
            } catch (IllegalArgumentException e) {
                log.warn("Failed to find exchange rate for {} on {}: {}",
                        currencyCode, transaction.getTransactionDate(), e.getMessage());
                throw new CurrencyConversionException(
                        String.format("Cannot convert to %s: %s", currencyCode, e.getMessage()), e);
            } catch (RuntimeException e) {
                log.error("Treasury API error while fetching exchange rate for {}: {}",
                        currencyCode, e.getMessage(), e);
                throw new CurrencyConversionException(
                        String.format("Treasury API unavailable. Cannot convert to %s", currencyCode), e);
            }

            // Calculate converted amount
            BigDecimal convertedAmount = convertAmount(
                    transaction.getAmountUsd(),
                    exchangeRate.getExchangeRate()
            );

            log.info("Transaction {} converted to {}: {} {} (rate: {}, date: {})",
                    transactionId, currencyCode, convertedAmount, currencyCode,
                    exchangeRate.getExchangeRate(), exchangeRate.getEffectiveDate());

            // Build and return response
            return PurchaseTransactionResponse.builder()
                    .id(transaction.getId())
                    .description(transaction.getDescription())
                    .transactionDate(transaction.getTransactionDate())
                    .amountUsd(transaction.getAmountUsd())
                    .currencyCode(currencyCode)
                    .exchangeRate(exchangeRate.getExchangeRate())
                    .exchangeRateDate(exchangeRate.getEffectiveDate())
                    .convertedAmount(convertedAmount)
                    .build();

        } catch (TransactionNotFoundException | CurrencyConversionException e) {
            // Re-throw business logic exceptions as-is
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error while retrieving transaction {}: {}", transactionId, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve transaction: " + e.getMessage(), e);
        }
    }

    /**
     * Convert an amount using an exchange rate
     * 
     * Formula: originalAmount * exchangeRate
     * Rounding: Standard financial rounding (HALF_UP) to 2 decimal places
     * 
     * @param originalAmount The amount to convert (typically in USD)
     * @param exchangeRate The exchange rate to apply
     * @return Converted amount rounded to 2 decimal places
     */
    private BigDecimal convertAmount(BigDecimal originalAmount, BigDecimal exchangeRate) {
        if (originalAmount == null || exchangeRate == null) {
            throw new IllegalArgumentException("Original amount and exchange rate cannot be null");
        }

        return originalAmount
                .multiply(exchangeRate)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
