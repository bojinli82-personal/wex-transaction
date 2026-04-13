package com.wex.transaction.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standard error response format for all API errors
 * 
 * This DTO provides a consistent error response structure across all endpoints:
 * - HTTP status code
 * - Error type/reason
 * - Detailed message
 * - Timestamp of the error
 * - API path that caused the error
 * - Optional field-level errors (for validation failures)
 * 
 * Example response:
 * {
 *   "status": 400,
 *   "error": "Validation Failed",
 *   "message": "Request body validation failed",
 *   "timestamp": "2024-01-15T10:30:00",
 *   "path": "/api/transactions",
 *   "fieldErrors": {
 *     "amountUsd": "Amount must be greater than 0"
 *   }
 * }
 * 
 * @author Bo
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /**
     * HTTP status code
     */
    private int status;

    /**
     * Error type or HTTP reason phrase (e.g., "Not Found", "Bad Request")
     */
    private String error;

    /**
     * Human-readable error message describing what went wrong
     */
    private String message;

    /**
     * Timestamp when the error occurred
     */
    private LocalDateTime timestamp;

    /**
     * The API path/endpoint that caused the error
     */
    private String path;

    /**
     * Optional field-level error messages (populated for validation errors)
     * Key: field name (e.g., "amountUsd")
     * Value: validation error message (e.g., "Amount must be greater than 0")
     */
    private Map<String, String> fieldErrors;
}
