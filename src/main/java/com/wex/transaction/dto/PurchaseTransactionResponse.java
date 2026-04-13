package com.wex.transaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data Transfer Object for retrieving a purchase transaction with currency conversion
 * 
 * This DTO is used to return transaction details to the API client, including
 * the original USD amount and the converted amount in the requested currency.
 * 
 * All fields are immutable after creation to ensure data integrity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseTransactionResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Unique identifier for the transaction
     */
    private String id;

    /**
     * Description of what was purchased
     */
    private String description;

    /**
     * The original date of the transaction
     */
    private LocalDate transactionDate;

    /**
     * Original purchase amount in United States dollars
     */
    private BigDecimal amountUsd;

    /**
     * The currency code for the conversion
     * Example: "CAD", "GBP", "EUR", etc.
     */
    private String currencyCode;

    /**
     * The exchange rate used for the currency conversion
     * This is the rate for the requested currency relative to USD on the transaction date
     */
    private BigDecimal exchangeRate;

    /**
     * The purchase amount converted to the requested currency
     * Calculated as: amountUsd * exchangeRate
     * Rounded to 2 decimal places
     */
    private BigDecimal convertedAmount;

    /**
     * The date the exchange rate was effective (may differ from transaction date)
     * This is the actual date from Treasury API for which the rate applies
     */
    private LocalDate exchangeRateDate;
}
