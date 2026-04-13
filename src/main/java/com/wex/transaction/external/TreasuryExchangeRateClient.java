package com.wex.transaction.external;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.wex.transaction.model.ExchangeRate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Treasury Exchange Rate API Client Service
 * 
 * This service handles interactions with the Treasury Reporting Rates of Exchange API.
 * It provides:
 * - Fetching exchange rates with automatic retry logic
 * - Local caching to handle API unavailability gracefully
 * - Searching for the most appropriate exchange rate within 6 months
 * 
 * For Production deployment:
 * - Implements retry logic with exponential backoff
 * - Caches exchange rates to provide resilience against API outages
 * - Logs all API interactions for monitoring and debugging
 * 
 * Decision: This implementation stores/caches exchange rates at retrieval time
 * to provide both resilience and audit trails for which rates were used.
 * 
 * @author Bo
 */
@Slf4j
@Service
public class TreasuryExchangeRateClient {

    private static final String CACHE_KEY_PATTERN = "%s_%s"; // currency_date
    private static final DateTimeFormatter API_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final RestTemplate restTemplate;
    private final String treasuryApiBaseUrl;
    private final String treasuryApiEndpoint;
    private final int maxRetries;
    private final int retryDelayMs;
    /**
     * In-memory cache for exchange rates
     * Key: "CURRENCY_DATE" (e.g., "GBP_2024-01-15")
     * Value: ExchangeRate object
     * 
     * This cache:
     * - Prevents repeated API calls for the same currency/date combination
     * - Survives the application's lifetime (memory-based)
     * - Provides resilience when Treasury API is temporarily unavailable
     * - Includes TTL to ensure stale data is eventually refreshed
     */
    private final Cache<String, List<ExchangeRate>> exchangeRateCache;

    public TreasuryExchangeRateClient(
            RestTemplate restTemplate,
            @Value("${treasury.api.base-url}") String baseUrl,
            @Value("${treasury.api.endpoint}") String endpoint,
            @Value("${treasury.api.timeout-seconds:15}") int timeoutSeconds,
            @Value("${treasury.api.max-retries:3}") int maxRetries,
            @Value("${treasury.api.retry-delay-ms:1000}") int retryDelayMs,
            @Value("${cache.exchange-rate.ttl-minutes:60}") int cacheExpirationMinutes,
            @Value("${cache.exchange-rate.max-size:1000}") int cacheMaxSize) {
        this.restTemplate = restTemplate;
        this.treasuryApiBaseUrl = baseUrl;
        this.treasuryApiEndpoint = endpoint;
        this.maxRetries = maxRetries;
        this.retryDelayMs = retryDelayMs;
        // Initialize the cache with TTL and max size
        this.exchangeRateCache = CacheBuilder.newBuilder()
                .expireAfterWrite(cacheExpirationMinutes, TimeUnit.MINUTES)
                .maximumSize(cacheMaxSize)
                .recordStats()
                .build();

        log.info("Treasury Exchange Rate Client initialized with base URL: {}, timeout: {}s, max retries: {}, cache TTL: {}min",
                baseUrl, timeoutSeconds, maxRetries, cacheExpirationMinutes);
    }

    /**
     * Get the exchange rate for a specific currency on or before a target date
     * 
     * This method will:
     * 1. Check the cache first
     * 2. If not in cache, fetch from Treasury API with retry logic
     * 3. Cache the result for future use
     * 4. Search for the best available rate within 6 months before the target date
     * 
     * @param currencyCode Currency code (ISO 4217, e.g., "GBP", "CAD")
     * @param targetDate The target date for the exchange rate
     * @return ExchangeRate containing the rate and date it was effective
     * @throws IllegalArgumentException if currency is not found within 6 months
     */
    public ExchangeRate getExchangeRate(String currencyCode, LocalDate targetDate) {
        if (currencyCode == null || currencyCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Currency code cannot be null or empty");
        }

        // Normalize currency code: trim, uppercase, validate format
        currencyCode = currencyCode.trim().toUpperCase();
        if (!currencyCode.matches("^[A-Z]{3}$")) {
            throw new IllegalArgumentException(
                    String.format("Invalid currency code format: %s. Must be 3-letter ISO 4217 code", currencyCode));
        }

        if (targetDate == null) {
            throw new IllegalArgumentException("Target date cannot be null");
        }

        log.debug("Fetching exchange rate for currency: {} on date: {}", currencyCode.toUpperCase(), targetDate);

        // Try direct cache lookup first
        String cacheKey = String.format(CACHE_KEY_PATTERN, currencyCode.toUpperCase(), targetDate);
        List<ExchangeRate> cachedRates = exchangeRateCache.getIfPresent(cacheKey);

        if (cachedRates != null && !cachedRates.isEmpty()) {
            log.debug("Found exchange rate in cache for {} on {}", currencyCode, targetDate);
            return cachedRates.get(0);
        }

        // Not in cache - fetch from API with retries
        List<ExchangeRate> rates = fetchExchangeRatesWithRetry(currencyCode, targetDate);

        if (rates.isEmpty()) {
            String errorMsg = String.format(
                    "No exchange rate found for currency %s within 6 months before %s",
                    currencyCode.toUpperCase(), targetDate);
            log.warn(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        // Cache the results
        exchangeRateCache.put(cacheKey, rates);

        // Return the most recent rate (Treasury API should return them in order)
        ExchangeRate bestRate = findBestExchangeRate(rates, targetDate);
        log.debug("Using exchange rate: {} = {} USD on {}", 
                bestRate.getCurrencyCode(), bestRate.getExchangeRate(), bestRate.getEffectiveDate());

        return bestRate;
    }

    /**
     * Fetch exchange rates from Treasury API with retry logic
     * 
     * Implements exponential backoff retry strategy:
     * - Initial delay: retryDelayMs (configurable)
     * - Each retry: previous delay * 2
     * - Maximum retries: configurable
     * 
     * @param currencyCode Currency code to fetch rates for
     * @param targetDate Target date for the rate search
     * @return List of exchange rates matching the criteria
     */
    private List<ExchangeRate> fetchExchangeRatesWithRetry(String currencyCode, LocalDate targetDate) {
        int attempt = 0;
        long delayMs = retryDelayMs;

        while (attempt <= maxRetries) {
            try {
                return fetchExchangeRatesFromApi(currencyCode, targetDate);
            } catch (RestClientException e) {
                attempt++;

                if (attempt > maxRetries) {
                    log.error("Failed to fetch exchange rates after {} attempts: {}", maxRetries, e.getMessage());
                    throw new RuntimeException(
                            "Treasury API unavailable. Unable to fetch exchange rate for " + currencyCode, e);
                }

                log.warn("Attempt {} failed, retrying in {}ms. Error: {}", attempt, delayMs, e.getMessage());

                try {
                    Thread.sleep(delayMs);
                    delayMs = Math.min(delayMs * 2, 32000); // Cap exponential backoff at 32 seconds
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while retrying Treasury API call", ie);
                }
            }
        }

        return Collections.emptyList();
    }

    /**
     * Fetch exchange rates directly from Treasury API
     * 
     * Constructs a query that filters for:
     * - Specific currency code
     * - Dates within the last 6 months before target date
     * 
     * API Query format:
     * ?filter=currency:eq:{currency},effective_date:gte:{startDate},effective_date:lte:{targetDate}
     * 
     * @param currencyCode Currency code (e.g., "GBP")
     * @param targetDate Maximum date for the rate search
     * @return List of exchange rates from API
     */
    private List<ExchangeRate> fetchExchangeRatesFromApi(String currencyCode, LocalDate targetDate) {
        // Calculate the 6-month lookback window
        LocalDate sixMonthsBefore = targetDate.minusMonths(6);

        // Build the API query URL with filters
        String url = String.format(
                "%s%s?filter=currency:eq:%s,effective_date:gte:%s,effective_date:lte:%s&sort=-effective_date&limit=1000",
                treasuryApiBaseUrl,
                treasuryApiEndpoint,
                currencyCode.toUpperCase(),
                sixMonthsBefore.format(API_DATE_FORMATTER),
                targetDate.format(API_DATE_FORMATTER)
        );

        log.debug("Querying Treasury API: {}", url);

        try {
            TreasuryApiResponse response = restTemplate.getForObject(url, TreasuryApiResponse.class);

            if (response == null || response.getData() == null || response.getData().isEmpty()) {
                log.debug("No exchange rates found in API response for {} between {} and {}",
                        currencyCode, sixMonthsBefore, targetDate);
                return Collections.emptyList();
            }

            // Convert API response to internal ExchangeRate objects
            List<ExchangeRate> rates = new ArrayList<>();
            for (TreasuryApiResponse.ExchangeRateData data : response.getData()) {
                try {
                    // Validate critical fields are not null
                    if (data.getCurrency() == null || data.getCurrency().trim().isEmpty()) {
                        log.warn("API returned record with null/empty currency");
                        continue;
                    }
                    if (data.getEffectiveDate() == null || data.getEffectiveDate().trim().isEmpty()) {
                        log.warn("API returned record with null/empty effective date");
                        continue;
                    }
                    if (data.getExchangeRate() == null || data.getExchangeRate().trim().isEmpty()) {
                        log.warn("API returned record with null/empty exchange rate for {}", data.getCurrency());
                        continue;
                    }

                    // Parse exchange rate and validate it's positive
                    BigDecimal parsedRate = new BigDecimal(data.getExchangeRate());
                    if (parsedRate.compareTo(BigDecimal.ZERO) <= 0) {
                        log.warn("API returned non-positive exchange rate {} for {}", parsedRate, data.getCurrency());
                        continue;
                    }

                    ExchangeRate rate = ExchangeRate.builder()
                            .currencyCode(data.getCurrency())
                            .effectiveDate(LocalDate.parse(data.getEffectiveDate(), API_DATE_FORMATTER))
                            .countryDescription(data.getCountryDescription())
                            .exchangeRate(parsedRate)
                            .recordId(data.getRecordId())
                            .id(data.getId())
                            .build();
                    rates.add(rate);
                } catch (NumberFormatException e) {
                    log.warn("Failed to parse exchange rate as number: {}. Error: {}", data.getExchangeRate(), e.getMessage());
                } catch (Exception e) {
                    log.warn("Failed to parse exchange rate record: {}", e.getMessage());
                }
            }

            log.debug("API returned {} exchange rates for currency: {}", rates.size(), currencyCode);
            return rates;

        } catch (RestClientException e) {
            log.error("Treasury API call failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Find the best exchange rate from a list
     * 
     * Selects the most recent rate that is on or before the target date.
     * 
     * @param rates List of exchange rates sorted by date (newest first expected)
     * @param targetDate The maximum date we want
     * @return The best matching exchange rate
     */
    private ExchangeRate findBestExchangeRate(List<ExchangeRate> rates, LocalDate targetDate) {
        // Sort by effective date descending (most recent first)
        return rates.stream()
                .filter(rate -> !rate.getEffectiveDate().isAfter(targetDate))
                .max(Comparator.comparing(ExchangeRate::getEffectiveDate))
                .orElseGet(() -> rates.get(0)); // Fallback to first if no perfect match
    }

    /**
     * Get cache statistics for monitoring
     * 
     * Useful for production monitoring to understand:
     * - Cache hit/miss ratio
     * - Number of cached items
     * 
     * @return String representation of cache statistics
     */
    public String getCacheStats() {
        return "Cache stats: " + exchangeRateCache.stats();
    }
}
