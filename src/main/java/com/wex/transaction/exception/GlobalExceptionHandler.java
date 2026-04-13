package com.wex.transaction.exception;

import com.wex.transaction.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler for REST API
 * 
 * This class handles exceptions globally across all controllers, providing:
 * - Consistent error response format
 * - Appropriate HTTP status codes
 * - Detailed error messages for debugging
 * - Proper logging for monitoring
 * 
 * All error responses follow this format:
 * {
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "Purchase transaction with ID xxx not found",
 *   "timestamp": "2024-01-15T10:30:00",
 *   "path": "/api/transactions/xxx"
 * }
 * 
 * @author Bo
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle TransactionNotFoundException
     * 
     * Thrown when a transaction with the requested ID is not found
     * 
     * HTTP Status: 404 Not Found
     * 
     * @param ex The exception that was thrown
     * @param request The current web request
     * @return Error response with 404 status
     */
    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTransactionNotFound(
            TransactionNotFoundException ex,
            WebRequest request) {
        
        log.warn("Transaction not found: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Handle CurrencyConversionException
     * 
     * Thrown when currency conversion cannot be performed due to:
     * - Unsupported currency code
     * - No exchange rate available within 6 months
     * - Treasury API unavailability
     * 
     * HTTP Status: 422 Unprocessable Entity
     * This status indicates the request was well-formed but cannot be processed
     * due to business logic constraints
     * 
     * @param ex The exception that was thrown
     * @param request The current web request
     * @return Error response with 422 status
     */
    @ExceptionHandler(CurrencyConversionException.class)
    public ResponseEntity<ErrorResponse> handleCurrencyConversionException(
            CurrencyConversionException ex,
            WebRequest request) {
        
        log.warn("Currency conversion failed: {}", ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .error("Conversion Failed")
                .message(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse);
    }

    /**
     * Handle validation errors (e.g., invalid request body)
     * 
     * Spring throws this exception when @Valid annotation validation fails
     * on request body parameters
     * 
     * HTTP Status: 400 Bad Request
     * 
     * Response includes detailed field-level error messages:
     * {
     *   "status": 400,
     *   "error": "Validation Failed",
     *   "message": "Request body validation failed",
     *   "timestamp": "...",
     *   "path": "...",
     *   "fieldErrors": {
     *     "description": "Description must not exceed 50 characters",
     *     "amount": "Amount must be greater than 0"
     *   }
     * }
     * 
     * @param ex The validation exception
     * @param request The current web request
     * @return Error response with 400 status and field-level errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            WebRequest request) {
        
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });
        
        log.warn("Validation error: {}", fieldErrors);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Request body validation failed")
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .fieldErrors(fieldErrors)
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Handle generic RuntimeException
     * 
     * Catches any unexpected runtime exceptions not handled by specific handlers
     * 
     * HTTP Status: 500 Internal Server Error
     * 
     * @param ex The exception that was thrown
     * @param request The current web request
     * @return Error response with 500 status
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException ex,
            WebRequest request) {
        
        log.error("Unexpected runtime error: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message("An unexpected error occurred. Please contact support.")
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Handle all other exceptions as a catch-all
     * 
     * HTTP Status: 500 Internal Server Error
     * 
     * @param ex The exception that was thrown
     * @param request The current web request
     * @return Error response with 500 status
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            WebRequest request) {
        
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("An unexpected error occurred. Please try again later.")
                .timestamp(LocalDateTime.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
