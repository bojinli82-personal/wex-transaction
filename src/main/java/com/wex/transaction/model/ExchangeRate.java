package com.wex.transaction.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Model representing an exchange rate record from the Treasury API
 * 
 * This class models the structure of exchange rate data returned by the
 * Treasury Reporting Rates of Exchange API.
 * 
 * API Documentation:
 * https://fiscaldata.treasury.gov/datasets/treasury-reporting-rates-exchange
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExchangeRate {

    /**
     * The date on which this exchange rate is effective
     */
    @JsonProperty("effective_date")
    private LocalDate effectiveDate;

    /**
     * The currency code (ISO 4217 format, e.g., "GBP", "CAD", "EUR")
     */
    @JsonProperty("currency")
    private String currencyCode;

    /**
     * The country or region name associated with this currency
     */
    @JsonProperty("country_currency_desc")
    private String countryDescription;

    /**
     * The exchange rate value
     * Represents how many units of this currency equal 1 US dollar
     * (or the fraction of a US dollar per unit, depending on the currency)
     * 
     * Constraints:
     * - Must be positive (greater than 0)
     * - Required (not null)
     */
    @JsonProperty("exchange_rate")
    @NotNull(message = "Exchange rate cannot be null")
    @DecimalMin(value = "0.0001", message = "Exchange rate must be greater than zero")
    private BigDecimal exchangeRate;

    /**
     * Record ID from the Treasury API (for tracking/auditing)
     */
    @JsonProperty("record_id")
    private String recordId;

    /**
     * Unique identifier for this record in the Treasury system
     */
    @JsonProperty("id")
    private String id;
}
