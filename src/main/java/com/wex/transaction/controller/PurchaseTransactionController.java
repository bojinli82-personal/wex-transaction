package com.wex.transaction.controller;

import com.wex.transaction.dto.CreatePurchaseTransactionRequest;
import com.wex.transaction.dto.CreatePurchaseTransactionResponse;
import com.wex.transaction.dto.PurchaseTransactionResponse;
import com.wex.transaction.service.PurchaseTransactionService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for Purchase Transaction endpoints
 * 
 * This controller exposes HTTP endpoints for:
 * - Creating new purchase transactions (POST /transactions)
 * - Retrieving transactions with currency conversion (GET /transactions/{id}?currency={code})
 * 
 * Key features:
 * - Automatic request validation using @Valid
 * - Proper HTTP status codes (201 for creation, 200 for retrieval)
 * - Comprehensive error handling via @ExceptionHandler methods
 * - Request/response logging for debugging
 * 
 * @author Bo
 */
@Slf4j
@RestController
@RequestMapping("/transactions")
public class PurchaseTransactionController {

    private final PurchaseTransactionService transactionService;

    /**
     * Constructor for dependency injection
     * 
     * @param transactionService Service layer for business logic
     */
    public PurchaseTransactionController(PurchaseTransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * Create a new purchase transaction
     * 
     * HTTP Method: POST
     * Endpoint: /transactions
     * 
     * Request body example:
     * {
     *   "description": "Office supplies purchase",
     *   "transactionDate": "2024-01-15",
     *   "amountUsd": 150.50
     * }
     * 
     * Response: HTTP 201 Created
     * {
     *   "id": "550e8400-e29b-41d4-a716-446655440000",
     *   "description": "Office supplies purchase",
     *   "transactionDate": "2024-01-15",
     *   "amountUsd": 150.50,
     *   "message": "Transaction created successfully"
     * }
     * 
     * Validation:
     * - @Valid triggers automatic validation of the request
     * - Spring returns 400 Bad Request if validation fails with detailed error messages
     * 
     * @param request The purchase transaction request with description, date, and amount
     * @return ResponseEntity with status 201 and the created transaction details
     */
    @PostMapping
    public ResponseEntity<CreatePurchaseTransactionResponse> createTransaction(
            @Valid @RequestBody CreatePurchaseTransactionRequest request) {
        
        log.info("Received request to create transaction: {}", request.getDescription());
        
        try {
            CreatePurchaseTransactionResponse response = transactionService.createTransaction(request);
            
            log.info("Transaction created successfully with ID: {}", response.getId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error creating transaction: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Retrieve a purchase transaction with currency conversion
     * 
     * HTTP Method: GET
     * Endpoint: /transactions/{id}?currency={currencyCode}
     * 
     * Path parameters:
     * - id: Unique identifier of the transaction to retrieve
     * 
     * Query parameters:
     * - currency: (Required) Currency code to convert to (ISO 4217, e.g., "GBP", "CAD", "EUR")
     * 
     * Example request:
     * GET /transactions/550e8400-e29b-41d4-a716-446655440000?currency=GBP
     * 
     * Response: HTTP 200 OK
     * {
     *   "id": "550e8400-e29b-41d4-a716-446655440000",
     *   "description": "Office supplies purchase",
     *   "transactionDate": "2024-01-15",
     *   "amountUsd": 150.50,
     *   "currencyCode": "GBP",
     *   "exchangeRate": 0.8124,
     *   "exchangeRateDate": "2024-01-15",
     *   "convertedAmount": 122.27
     * }
     * 
     * Error responses:
     * - 404 Not Found: Transaction doesn't exist
     * - 422 Unprocessable Entity: Currency conversion failed (rate not available)
     * - 500 Internal Server Error: Unexpected server error
     * 
     * @param transactionId The unique identifier of the transaction to retrieve
     * @param currencyCode The target currency code for conversion
     * @return ResponseEntity with status 200 and the transaction with converted amount
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<PurchaseTransactionResponse> getTransaction(
            @PathVariable String transactionId,
            @RequestParam(required = true) String currencyCode) {
        
        log.info("Received request to retrieve transaction: {} with currency: {}", transactionId, currencyCode);
        
        try {
            PurchaseTransactionResponse response = transactionService.getTransactionWithConversion(
                    transactionId, currencyCode);
            
            log.info("Transaction retrieved successfully: {}", transactionId);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving transaction: {}", e.getMessage(), e);
            throw e;
        }
    }
}
