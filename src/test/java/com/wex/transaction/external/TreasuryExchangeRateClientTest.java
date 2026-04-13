package com.wex.transaction.external;

import com.wex.transaction.model.ExchangeRate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentCaptor;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for TreasuryExchangeRateClient
 * 
 * Tests cover:
 * - Successful API calls and rate retrieval
 * - Retry logic with exponential backoff
 * - Caching of exchange rates
 * - Error handling and fallback behavior
 * - 6-month lookback window enforcement
 * - Rate selection logic (most recent rate)
 * 
 * @author Bo
 */
@DisplayName("TreasuryExchangeRateClient Unit Tests")
class TreasuryExchangeRateClientTest {

    private TreasuryExchangeRateClient client;
    private RestTemplate restTemplateMock;

    @BeforeEach
    void setup() {
        restTemplateMock = mock(RestTemplate.class);
        client = new TreasuryExchangeRateClient(
                restTemplateMock,
                "https://api.test.com",
                "/rates",
                15,
                3,
                100,
                60,
                1000
        );
    }

    /**
     * Test: Successfully fetch exchange rate from API
     * Given: Valid currency code and API returns data
     * When: getExchangeRate is called
     * Then: ExchangeRate object is returned with correct values
     */
    @Test
    @DisplayName("Should fetch exchange rate successfully")
    void testFetchExchangeRateSuccess() {
        // Arrange
        String currencyCode = "GBP";
        LocalDate targetDate = LocalDate.of(2024, 1, 15);

        TreasuryApiResponse.ExchangeRateData rateData = new TreasuryApiResponse.ExchangeRateData();
        rateData.setCurrency("GBP");
        rateData.setEffectiveDate("2024-01-15");
        rateData.setExchangeRate("0.8124");
        rateData.setCountryDescription("United Kingdom");
        rateData.setRecordId("123");
        rateData.setId("456");

        TreasuryApiResponse response = new TreasuryApiResponse();
        response.setData(List.of(rateData));

        when(restTemplateMock.getForObject(contains(currencyCode), eq(TreasuryApiResponse.class)))
                .thenReturn(response);

        // Act
        ExchangeRate result = client.getExchangeRate(currencyCode, targetDate);

        // Assert
        assertNotNull(result);
        assertEquals("GBP", result.getCurrencyCode());
        assertEquals(LocalDate.of(2024, 1, 15), result.getEffectiveDate());
        assertEquals(new BigDecimal("0.8124"), result.getExchangeRate());
    }

    /**
     * Test: Cache hit on second call
     * Given: Same currency and date called twice
     * When: getExchangeRate is called two times
     * Then: API is called only once (second call uses cache)
     */
    @Test
    @DisplayName("Should cache exchange rates to avoid redundant API calls")
    void testExchangeRateCaching() {
        // Arrange
        String currencyCode = "EUR";
        LocalDate targetDate = LocalDate.of(2024, 1, 15);

        TreasuryApiResponse.ExchangeRateData rateData = new TreasuryApiResponse.ExchangeRateData();
        rateData.setCurrency("EUR");
        rateData.setEffectiveDate("2024-01-15");
        rateData.setExchangeRate("1.0500");

        TreasuryApiResponse response = new TreasuryApiResponse();
        response.setData(List.of(rateData));

        when(restTemplateMock.getForObject(anyString(), eq(TreasuryApiResponse.class)))
                .thenReturn(response);

        // Act - Call twice
        ExchangeRate result1 = client.getExchangeRate(currencyCode, targetDate);
        ExchangeRate result2 = client.getExchangeRate(currencyCode, targetDate);

        // Assert
        assertEquals(result1.getExchangeRate(), result2.getExchangeRate());
        verify(restTemplateMock, times(1)).getForObject(anyString(), eq(TreasuryApiResponse.class));
    }

    /**
     * Test: API returns multiple rates, most recent is selected
     * Given: API returns multiple exchange rates for different dates
     * When: getExchangeRate is called
     * Then: Most recent rate on or before target date is selected
     */
    @Test
    @DisplayName("Should select most recent exchange rate within date range")
    void testSelectMostRecentExchangeRate() {
        // Arrange
        String currencyCode = "CAD";
        LocalDate targetDate = LocalDate.of(2024, 1, 20);

        // Create multiple rates for different dates
        TreasuryApiResponse.ExchangeRateData rate1 = new TreasuryApiResponse.ExchangeRateData();
        rate1.setCurrency("CAD");
        rate1.setEffectiveDate("2024-01-10");
        rate1.setExchangeRate("1.3000");

        TreasuryApiResponse.ExchangeRateData rate2 = new TreasuryApiResponse.ExchangeRateData();
        rate2.setCurrency("CAD");
        rate2.setEffectiveDate("2024-01-15");
        rate2.setExchangeRate("1.3100");

        TreasuryApiResponse.ExchangeRateData rate3 = new TreasuryApiResponse.ExchangeRateData();
        rate3.setCurrency("CAD");
        rate3.setEffectiveDate("2024-01-20");
        rate3.setExchangeRate("1.3200");

        TreasuryApiResponse response = new TreasuryApiResponse();
        response.setData(List.of(rate1, rate2, rate3));

        when(restTemplateMock.getForObject(anyString(), eq(TreasuryApiResponse.class)))
                .thenReturn(response);

        // Act
        ExchangeRate result = client.getExchangeRate(currencyCode, targetDate);

        // Assert - Should get the most recent rate (1.3200 on 2024-01-20)
        assertEquals(new BigDecimal("1.3200"), result.getExchangeRate());
        assertEquals(LocalDate.of(2024, 1, 20), result.getEffectiveDate());
    }

    /**
     * Test: Currency code is case-insensitive
     * Given: Lowercase currency code
     * When: getExchangeRate is called
     * Then: API query contains uppercase currency code
     */
    @Test
    @DisplayName("Should handle case-insensitive currency codes")
    void testCaseInsensitiveCurrencyCode() {
        // Arrange
        String currencyCode = "gbp"; // lowercase
        LocalDate targetDate = LocalDate.of(2024, 1, 15);

        TreasuryApiResponse.ExchangeRateData rateData = new TreasuryApiResponse.ExchangeRateData();
        rateData.setCurrency("GBP");
        rateData.setEffectiveDate("2024-01-15");
        rateData.setExchangeRate("0.8124");

        TreasuryApiResponse response = new TreasuryApiResponse();
        response.setData(List.of(rateData));

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);

        when(restTemplateMock.getForObject(urlCaptor.capture(), eq(TreasuryApiResponse.class)))
                .thenReturn(response);

        // Act
        ExchangeRate result = client.getExchangeRate(currencyCode, targetDate);

        // Assert
        assertNotNull(result);
        String capturedUrl = urlCaptor.getValue();
        assertTrue(capturedUrl.contains("GBP"), "Currency code should be converted to uppercase in API call");
    }

    /**
     * Test: Invalid currency throws IllegalArgumentException
     * Given: Empty currency code
     * When: getExchangeRate is called
     * Then: IllegalArgumentException is thrown
     */
    @Test
    @DisplayName("Should throw IllegalArgumentException for null currency code")
    void testNullCurrencyCode() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                client.getExchangeRate(null, LocalDate.of(2024, 1, 15))
        );
    }

    /**
     * Test: No exchange rate found within 6 months
     * Given: API returns empty data
     * When: getExchangeRate is called
     * Then: IllegalArgumentException with descriptive message
     */
    @Test
    @DisplayName("Should throw exception when no rate found in 6-month window")
    void testNoExchangeRateWithin6Months() {
        // Arrange
        String currencyCode = "XXX";
        LocalDate targetDate = LocalDate.of(2024, 1, 15);

        TreasuryApiResponse response = new TreasuryApiResponse();
        response.setData(new ArrayList<>()); // Empty response

        when(restTemplateMock.getForObject(anyString(), eq(TreasuryApiResponse.class)))
                .thenReturn(response);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                client.getExchangeRate(currencyCode, targetDate)
        );

        assertTrue(exception.getMessage().contains("No exchange rate found"));
    }

    /**
     * Test: API retry logic with temporary failure then success
     * Given: API fails on first call but succeeds on retry
     * When: getExchangeRate is called
     * Then: Retry logic kicks in and successful result is returned
     */
    @Test
    @DisplayName("Should retry failed API calls")
    void testRetryLogicOnTemporaryFailure() throws InterruptedException {
        // Arrange
        String currencyCode = "JPY";
        LocalDate targetDate = LocalDate.of(2024, 1, 15);

        TreasuryApiResponse.ExchangeRateData rateData = new TreasuryApiResponse.ExchangeRateData();
        rateData.setCurrency("JPY");
        rateData.setEffectiveDate("2024-01-15");
        rateData.setExchangeRate("150.0000");

        TreasuryApiResponse response = new TreasuryApiResponse();
        response.setData(List.of(rateData));

        // First call fails, second succeeds
        when(restTemplateMock.getForObject(anyString(), eq(TreasuryApiResponse.class)))
                .thenThrow(new RestClientException("Connection timeout"))
                .thenReturn(response);

        // Act
        ExchangeRate result = client.getExchangeRate(currencyCode, targetDate);

        // Assert
        assertNotNull(result);
        assertEquals(new BigDecimal("150.0000"), result.getExchangeRate());
        verify(restTemplateMock, times(2)).getForObject(anyString(), eq(TreasuryApiResponse.class));
    }

    /**
     * Test: API exhausts all retries and fails
     * Given: API fails on all retry attempts
     * When: getExchangeRate is called
     * Then: RuntimeException is thrown after all retries exhausted
     */
    @Test
    @DisplayName("Should throw exception after exhausting retries")
    void testExhaustedRetries() {
        // Arrange
        String currencyCode = "XXX";
        LocalDate targetDate = LocalDate.of(2024, 1, 15);

        when(restTemplateMock.getForObject(anyString(), eq(TreasuryApiResponse.class)))
                .thenThrow(new RestClientException("Connection refused"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                client.getExchangeRate(currencyCode, targetDate)
        );

        assertTrue(exception.getMessage().contains("Treasury API unavailable"));
        verify(restTemplateMock, atLeast(1)).getForObject(anyString(), eq(TreasuryApiResponse.class));
    }

    /**
     * Test: API query includes proper date filtering
     * Given: Target date and 6-month lookback window
     * When: getExchangeRate is called
     * Then: API query is constructed with correct date range
     */
    @Test
    @DisplayName("Should query API with correct 6-month date range")
    void testCorrectDateRangeInQuery() {
        // Arrange
        String currencyCode = "CHF";
        LocalDate targetDate = LocalDate.of(2024, 1, 15);
        TreasuryApiResponse.ExchangeRateData rateData = new TreasuryApiResponse.ExchangeRateData();
        rateData.setCurrency("CHF");
        rateData.setEffectiveDate("2024-01-15");
        rateData.setExchangeRate("0.9200");

        TreasuryApiResponse response = new TreasuryApiResponse();
        response.setData(List.of(rateData));

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);

        when(restTemplateMock.getForObject(urlCaptor.capture(), eq(TreasuryApiResponse.class)))
                .thenReturn(response);

        // Act
        client.getExchangeRate(currencyCode, targetDate);

        // Assert
        String capturedUrl = urlCaptor.getValue();
        assertTrue(capturedUrl.contains("2023-07-15"), "Query should include 6-month lookback date");
        assertTrue(capturedUrl.contains("2024-01-15"), "Query should include target date");
    }

    // ===== EDGE CASE TESTS: CURRENCY CODE VALIDATION =====

    @Test
    @DisplayName("Should reject empty currency code")
    void testEmptyCurrencyCode() {
        LocalDate targetDate = LocalDate.of(2024, 1, 15);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                client.getExchangeRate("", targetDate)
        );
        assertEquals("Currency code cannot be null or empty", ex.getMessage());
    }

    @Test
    @DisplayName("Should reject whitespace-only currency code")
    void testWhitespaceCurrencyCode() {
        LocalDate targetDate = LocalDate.of(2024, 1, 15);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                client.getExchangeRate("   ", targetDate)
        );
        assertEquals("Currency code cannot be null or empty", ex.getMessage());
    }

    @Test
    @DisplayName("Should reject currency code with less than 3 letters")
    void testTwoLetterCurrencyCode() {
        LocalDate targetDate = LocalDate.of(2024, 1, 15);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                client.getExchangeRate("GB", targetDate)
        );
        assertEquals("Invalid currency code format: GB. Must be 3-letter ISO 4217 code", ex.getMessage());
    }

    @Test
    @DisplayName("Should reject currency code with more than 3 letters")
    void testFourLetterCurrencyCode() {
        LocalDate targetDate = LocalDate.of(2024, 1, 15);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                client.getExchangeRate("GBPX", targetDate)
        );
        assertEquals("Invalid currency code format: GBPX. Must be 3-letter ISO 4217 code", ex.getMessage());
    }

    @Test
    @DisplayName("Should reject currency code with numbers")
    void testCurrencyCodeWithNumbers() {
        LocalDate targetDate = LocalDate.of(2024, 1, 15);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                client.getExchangeRate("GB1", targetDate)
        );
        assertEquals("Invalid currency code format: GB1. Must be 3-letter ISO 4217 code", ex.getMessage());
    }

    @Test
    @DisplayName("Should reject currency code with special characters")
    void testCurrencyCodeWithSpecialChars() {
        LocalDate targetDate = LocalDate.of(2024, 1, 15);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                client.getExchangeRate("G@P", targetDate)
        );
        assertEquals("Invalid currency code format: G@P. Must be 3-letter ISO 4217 code", ex.getMessage());
    }

    @Test
    @DisplayName("Should handle lowercase currency code by normalizing to uppercase")
    void testLowercaseCurrencyCodeNormalization() {
        String currencyCode = "gbp";
        LocalDate targetDate = LocalDate.of(2024, 1, 15);

        TreasuryApiResponse.ExchangeRateData rateData = new TreasuryApiResponse.ExchangeRateData();
        rateData.setCurrency("GBP");
        rateData.setEffectiveDate("2024-01-15");
        rateData.setExchangeRate("0.8124");
        rateData.setCountryDescription("United Kingdom");

        TreasuryApiResponse response = new TreasuryApiResponse();
        response.setData(List.of(rateData));

        when(restTemplateMock.getForObject(contains("GBP"), eq(TreasuryApiResponse.class)))
                .thenReturn(response);

        ExchangeRate result = client.getExchangeRate(currencyCode, targetDate);

        assertNotNull(result);
        assertEquals("GBP", result.getCurrencyCode());
    }

    @Test
    @DisplayName("Should handle currency code with leading/trailing whitespace")
    void testCurrencyCodeWithWhitespace() {
        String currencyCode = "  GBP  ";
        LocalDate targetDate = LocalDate.of(2024, 1, 15);

        TreasuryApiResponse.ExchangeRateData rateData = new TreasuryApiResponse.ExchangeRateData();
        rateData.setCurrency("GBP");
        rateData.setEffectiveDate("2024-01-15");
        rateData.setExchangeRate("0.8124");

        TreasuryApiResponse response = new TreasuryApiResponse();
        response.setData(List.of(rateData));

        when(restTemplateMock.getForObject(contains("GBP"), eq(TreasuryApiResponse.class)))
                .thenReturn(response);

        ExchangeRate result = client.getExchangeRate(currencyCode, targetDate);

        assertNotNull(result);
        assertEquals("GBP", result.getCurrencyCode());
    }

    // ===== EDGE CASE TESTS: API RESPONSE HANDLING =====

    @Test
    @DisplayName("Should skip API record with null currency")
    void testApiResponseWithNullCurrency() {
        String currencyCode = "GBP";
        LocalDate targetDate = LocalDate.of(2024, 1, 15);

        TreasuryApiResponse.ExchangeRateData validData = new TreasuryApiResponse.ExchangeRateData();
        validData.setCurrency("GBP");
        validData.setEffectiveDate("2024-01-15");
        validData.setExchangeRate("0.8124");

        TreasuryApiResponse.ExchangeRateData nullCurrencyData = new TreasuryApiResponse.ExchangeRateData();
        nullCurrencyData.setCurrency(null);
        nullCurrencyData.setEffectiveDate("2024-01-14");
        nullCurrencyData.setExchangeRate("0.8120");

        TreasuryApiResponse response = new TreasuryApiResponse();
        response.setData(List.of(nullCurrencyData, validData));

        when(restTemplateMock.getForObject(contains(currencyCode), eq(TreasuryApiResponse.class)))
                .thenReturn(response);

        ExchangeRate result = client.getExchangeRate(currencyCode, targetDate);

        assertNotNull(result);
        assertEquals("GBP", result.getCurrencyCode());
        assertEquals(new BigDecimal("0.8124"), result.getExchangeRate());
    }

    @Test
    @DisplayName("Should skip API record with null effective date")
    void testApiResponseWithNullEffectiveDate() {
        String currencyCode = "GBP";
        LocalDate targetDate = LocalDate.of(2024, 1, 15);

        TreasuryApiResponse.ExchangeRateData validData = new TreasuryApiResponse.ExchangeRateData();
        validData.setCurrency("GBP");
        validData.setEffectiveDate("2024-01-15");
        validData.setExchangeRate("0.8124");

        TreasuryApiResponse.ExchangeRateData nullDateData = new TreasuryApiResponse.ExchangeRateData();
        nullDateData.setCurrency("GBP");
        nullDateData.setEffectiveDate(null);
        nullDateData.setExchangeRate("0.8120");

        TreasuryApiResponse response = new TreasuryApiResponse();
        response.setData(List.of(nullDateData, validData));

        when(restTemplateMock.getForObject(contains(currencyCode), eq(TreasuryApiResponse.class)))
                .thenReturn(response);

        ExchangeRate result = client.getExchangeRate(currencyCode, targetDate);

        assertNotNull(result);
        assertEquals("GBP", result.getCurrencyCode());
        assertEquals(new BigDecimal("0.8124"), result.getExchangeRate());
    }

    @Test
    @DisplayName("Should skip API record with null exchange rate")
    void testApiResponseWithNullExchangeRate() {
        String currencyCode = "GBP";
        LocalDate targetDate = LocalDate.of(2024, 1, 15);

        TreasuryApiResponse.ExchangeRateData validData = new TreasuryApiResponse.ExchangeRateData();
        validData.setCurrency("GBP");
        validData.setEffectiveDate("2024-01-15");
        validData.setExchangeRate("0.8124");

        TreasuryApiResponse.ExchangeRateData nullRateData = new TreasuryApiResponse.ExchangeRateData();
        nullRateData.setCurrency("GBP");
        nullRateData.setEffectiveDate("2024-01-14");
        nullRateData.setExchangeRate(null);

        TreasuryApiResponse response = new TreasuryApiResponse();
        response.setData(List.of(nullRateData, validData));

        when(restTemplateMock.getForObject(contains(currencyCode), eq(TreasuryApiResponse.class)))
                .thenReturn(response);

        ExchangeRate result = client.getExchangeRate(currencyCode, targetDate);

        assertNotNull(result);
        assertEquals("GBP", result.getCurrencyCode());
        assertEquals(new BigDecimal("0.8124"), result.getExchangeRate());
    }

    @Test
    @DisplayName("Should skip API record with zero exchange rate")
    void testApiResponseWithZeroExchangeRate() {
        String currencyCode = "GBP";
        LocalDate targetDate = LocalDate.of(2024, 1, 15);

        TreasuryApiResponse.ExchangeRateData validData = new TreasuryApiResponse.ExchangeRateData();
        validData.setCurrency("GBP");
        validData.setEffectiveDate("2024-01-15");
        validData.setExchangeRate("0.8124");

        TreasuryApiResponse.ExchangeRateData zeroRateData = new TreasuryApiResponse.ExchangeRateData();
        zeroRateData.setCurrency("GBP");
        zeroRateData.setEffectiveDate("2024-01-14");
        zeroRateData.setExchangeRate("0");

        TreasuryApiResponse response = new TreasuryApiResponse();
        response.setData(List.of(zeroRateData, validData));

        when(restTemplateMock.getForObject(contains(currencyCode), eq(TreasuryApiResponse.class)))
                .thenReturn(response);

        ExchangeRate result = client.getExchangeRate(currencyCode, targetDate);

        assertNotNull(result);
        assertEquals("GBP", result.getCurrencyCode());
        assertEquals(new BigDecimal("0.8124"), result.getExchangeRate());
    }

    @Test
    @DisplayName("Should skip API record with negative exchange rate")
    void testApiResponseWithNegativeExchangeRate() {
        String currencyCode = "GBP";
        LocalDate targetDate = LocalDate.of(2024, 1, 15);

        TreasuryApiResponse.ExchangeRateData validData = new TreasuryApiResponse.ExchangeRateData();
        validData.setCurrency("GBP");
        validData.setEffectiveDate("2024-01-15");
        validData.setExchangeRate("0.8124");

        TreasuryApiResponse.ExchangeRateData negativeRateData = new TreasuryApiResponse.ExchangeRateData();
        negativeRateData.setCurrency("GBP");
        negativeRateData.setEffectiveDate("2024-01-14");
        negativeRateData.setExchangeRate("-0.8120");

        TreasuryApiResponse response = new TreasuryApiResponse();
        response.setData(List.of(negativeRateData, validData));

        when(restTemplateMock.getForObject(contains(currencyCode), eq(TreasuryApiResponse.class)))
                .thenReturn(response);

        ExchangeRate result = client.getExchangeRate(currencyCode, targetDate);

        assertNotNull(result);
        assertEquals("GBP", result.getCurrencyCode());
        assertEquals(new BigDecimal("0.8124"), result.getExchangeRate());
    }

    @Test
    @DisplayName("Should handle malformed exchange rate number")
    void testApiResponseWithMalformedExchangeRate() {
        String currencyCode = "GBP";
        LocalDate targetDate = LocalDate.of(2024, 1, 15);

        TreasuryApiResponse.ExchangeRateData validData = new TreasuryApiResponse.ExchangeRateData();
        validData.setCurrency("GBP");
        validData.setEffectiveDate("2024-01-15");
        validData.setExchangeRate("0.8124");

        TreasuryApiResponse.ExchangeRateData malformedData = new TreasuryApiResponse.ExchangeRateData();
        malformedData.setCurrency("GBP");
        malformedData.setEffectiveDate("2024-01-14");
        malformedData.setExchangeRate("invalid");

        TreasuryApiResponse response = new TreasuryApiResponse();
        response.setData(List.of(malformedData, validData));

        when(restTemplateMock.getForObject(contains(currencyCode), eq(TreasuryApiResponse.class)))
                .thenReturn(response);

        ExchangeRate result = client.getExchangeRate(currencyCode, targetDate);

        assertNotNull(result);
        assertEquals("GBP", result.getCurrencyCode());
        assertEquals(new BigDecimal("0.8124"), result.getExchangeRate());
    }

    @Test
    @DisplayName("Should reject null target date")
    void testNullTargetDate() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                client.getExchangeRate("GBP", null)
        );
        assertEquals("Target date cannot be null", ex.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when all API records are invalid")
    void testAllApiRecordsInvalid() {
        String currencyCode = "GBP";
        LocalDate targetDate = LocalDate.of(2024, 1, 15);

        TreasuryApiResponse.ExchangeRateData zeroRate = new TreasuryApiResponse.ExchangeRateData();
        zeroRate.setCurrency("GBP");
        zeroRate.setEffectiveDate("2024-01-15");
        zeroRate.setExchangeRate("0");

        TreasuryApiResponse.ExchangeRateData negativeRate = new TreasuryApiResponse.ExchangeRateData();
        negativeRate.setCurrency("GBP");
        negativeRate.setEffectiveDate("2024-01-14");
        negativeRate.setExchangeRate("-1.5");

        TreasuryApiResponse response = new TreasuryApiResponse();
        response.setData(List.of(zeroRate, negativeRate));

        when(restTemplateMock.getForObject(contains(currencyCode), eq(TreasuryApiResponse.class)))
                .thenReturn(response);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                client.getExchangeRate(currencyCode, targetDate)
        );
        assertEquals("No exchange rate found for currency GBP within 6 months before " + targetDate, ex.getMessage());
    }
}
