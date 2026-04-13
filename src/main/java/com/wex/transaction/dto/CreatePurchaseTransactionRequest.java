package com.wex.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data Transfer Object for creating a new purchase transaction
 * 
 * This DTO is used to validate and transfer purchase transaction data
 * from the API request to the service layer.
 * 
 * All validation constraints are applied here to ensure only valid
 * data is processed by the business logic.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePurchaseTransactionRequest {

    /**
     * Description of the purchase item/service
     * Constraints:
     * - Required
     * - Maximum 50 characters
     * - Must not be blank
     * - Whitespace will be trimmed
     */
    @NotBlank(message = "Description is required")
    @Size(max = 50, message = "Description must not exceed 50 characters")
    private String description;

    /**
     * The date when the transaction occurred
     * Constraints:
     * - Required
     * - Cannot be in the future
     */
    @NotNull(message = "Transaction date is required")
    @PastOrPresent(message = "Transaction date cannot be in the future")
    private LocalDate transactionDate;

    /**
     * The purchase amount in US dollars
     * Constraints:
     * - Required
     * - Must be positive (greater than 0)
     * - Maximum 15 integer digits and 2 decimal places
     */
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 15, fraction = 2, message = "Amount must have at most 15 integer digits and 2 decimal places")
    private BigDecimal amountUsd;
}
