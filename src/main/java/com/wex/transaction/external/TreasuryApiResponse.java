package com.wex.transaction.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Treasury API response wrapper for exchange rate data
 * 
 * The Treasury API returns exchange rate data wrapped in a data array.
 * This class models that response structure.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TreasuryApiResponse {

    /**
     * Array of exchange rate records
     */
    @JsonProperty("data")
    private List<ExchangeRateData> data;

    /**
     * Metadata about the API response (pagination, filtering, etc.)
     */
    @JsonProperty("meta")
    private ApiMetadata meta;

    /**
     * Inner class representing individual exchange rate record from API
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExchangeRateData {

        @JsonProperty("effective_date")
        private String effectiveDate;

        @JsonProperty("currency")
        private String currency;

        @JsonProperty("country_currency_desc")
        private String countryDescription;

        @JsonProperty("exchange_rate")
        private String exchangeRate;

        @JsonProperty("record_id")
        private String recordId;

        @JsonProperty("id")
        private String id;
    }

    /**
     * Inner class for API metadata
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ApiMetadata {

        @JsonProperty("total-records")
        private Integer totalRecords;

        @JsonProperty("total-pages")
        private Integer totalPages;
    }
}
