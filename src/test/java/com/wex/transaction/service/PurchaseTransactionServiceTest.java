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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PurchaseTransactionService
 * 
 * Tests cover:
 * - Transaction creation and persistence
 * - Transaction retrieval with currency conversion
 * - Error handling for various failure scenarios
 * - Currency conversion calculations
 * - Integration with Treasury API client
 * 
 * @author Bo
 */
@DisplayName("PurchaseTransactionService Unit Tests")
class PurchaseTransactionServiceTest {

    private PurchaseTransactionService service;

    @Mock
    private PurchaseTransactionRepository transactionRepository;

    @Mock
    private TreasuryExchangeRateClient exchangeRateClient;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        service = new PurchaseTransactionService(transactionRepository, exchangeRateClient);
    }

    /**
     * Test: Successfully create a new transaction
     * Given: Valid transaction request
     * When: createTransaction is called
     * Then: Transaction is saved and ID is returned
     */
    @Test
    @DisplayName("Should create transaction with valid input")
    void testCreateTransactionSuccess() {
        // Arrange
        CreatePurchaseTransactionRequest request = CreatePurchaseTransactionRequest.builder()
                .description("Office supplies")
                .transactionDate(LocalDate.of(2024, 1, 15))
                .amountUsd(new BigDecimal("150.50"))
                .build();

        PurchaseTransaction savedTransaction = PurchaseTransaction.builder()
                .id("550e8400-e29b-41d4-a716-446655440000")
                .description("Office supplies")
                .transactionDate(LocalDate.of(2024, 1, 15))
                .amountUsd(new BigDecimal("150.50"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(transactionRepository.save(any(PurchaseTransaction.class))).thenReturn(savedTransaction);

        // Act
        CreatePurchaseTransactionResponse response = service.createTransaction(request);

        // Assert
        assertNotNull(response);
        assertEquals("550e8400-e29b-41d4-a716-446655440000", response.getId());
        assertEquals("Office supplies", response.getDescription());
        assertEquals(LocalDate.of(2024, 1, 15), response.getTransactionDate());
        assertEquals(new BigDecimal("150.50"), response.getAmountUsd());
        verify(transactionRepository, times(1)).save(any(PurchaseTransaction.class));
    }

    /**
     * Test: Successfully retrieve transaction and convert to different currency
     * Given: Valid transaction ID and currency code
     * When: getTransactionWithConversion is called
     * Then: Transaction details with converted amount are returned
     */
    @Test
    @DisplayName("Should retrieve transaction with currency conversion")
    void testGetTransactionWithConversionSuccess() {
        // Arrange
        String transactionId = "550e8400-e29b-41d4-a716-446655440000";
        String currencyCode = "GBP";

        PurchaseTransaction transaction = PurchaseTransaction.builder()
                .id(transactionId)
                .description("Office supplies")
                .transactionDate(LocalDate.of(2024, 1, 15))
                .amountUsd(new BigDecimal("150.50"))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ExchangeRate exchangeRate = ExchangeRate.builder()
                .currencyCode("GBP")
                .effectiveDate(LocalDate.of(2024, 1, 15))
                .exchangeRate(new BigDecimal("0.8124"))
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(exchangeRateClient.getExchangeRate(currencyCode, LocalDate.of(2024, 1, 15)))
                .thenReturn(exchangeRate);

        // Act
        PurchaseTransactionResponse response = service.getTransactionWithConversion(transactionId, currencyCode);

        // Assert
        assertNotNull(response);
        assertEquals(transactionId, response.getId());
        assertEquals("Office supplies", response.getDescription());
        assertEquals(new BigDecimal("150.50"), response.getAmountUsd());
        assertEquals("GBP", response.getCurrencyCode());
        assertEquals(new BigDecimal("0.8124"), response.getExchangeRate());
        // Converted amount: 150.50 * 0.8124 = 122.27
        assertEquals(new BigDecimal("122.27"), response.getConvertedAmount());
        verify(exchangeRateClient, times(1)).getExchangeRate(currencyCode, LocalDate.of(2024, 1, 15));
    }

    /**
     * Test: Throw TransactionNotFoundException when transaction doesn't exist
     * Given: Non-existent transaction ID
     * When: getTransactionWithConversion is called
     * Then: TransactionNotFoundException is thrown
     */
    @Test
    @DisplayName("Should throw TransactionNotFoundException when transaction not found")
    void testGetTransactionNotFound() {
        // Arrange
        String transactionId = "non-existent-id";
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(TransactionNotFoundException.class, () ->
                service.getTransactionWithConversion(transactionId, "GBP")
        );
        verify(exchangeRateClient, never()).getExchangeRate(anyString(), any());
    }

    /**
     * Test: Throw CurrencyConversionException when exchange rate cannot be found
     * Given: Valid transaction but unsupported currency
     * When: getTransactionWithConversion is called
     * Then: CurrencyConversionException is thrown
     */
    @Test
    @DisplayName("Should throw CurrencyConversionException when exchange rate not found")
    void testGetTransactionCurrencyConversionFailed() {
        // Arrange
        String transactionId = "550e8400-e29b-41d4-a716-446655440000";
        String currencyCode = "ZZZ";  // Valid 3-letter code but not a real currency

        PurchaseTransaction transaction = PurchaseTransaction.builder()
                .id(transactionId)
                .description("Office supplies")
                .transactionDate(LocalDate.of(2024, 1, 15))
                .amountUsd(new BigDecimal("150.50"))
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(exchangeRateClient.getExchangeRate(currencyCode, LocalDate.of(2024, 1, 15)))
                .thenThrow(new IllegalArgumentException("No exchange rate found for ZZZ"));

        // Act & Assert
        assertThrows(CurrencyConversionException.class, () ->
                service.getTransactionWithConversion(transactionId, currencyCode)
        );
    }

    /**
     * Test: Throw CurrencyConversionException when Treasury API is unavailable
     * Given: Valid transaction and currency, but API is down
     * When: getTransactionWithConversion is called
     * Then: CurrencyConversionException is thrown with appropriate message
     */
    @Test
    @DisplayName("Should throw CurrencyConversionException when Treasury API is unavailable")
    void testGetTransactionTreasuryApiUnavailable() {
        // Arrange
        String transactionId = "550e8400-e29b-41d4-a716-446655440000";
        String currencyCode = "GBP";

        PurchaseTransaction transaction = PurchaseTransaction.builder()
                .id(transactionId)
                .description("Office supplies")
                .transactionDate(LocalDate.of(2024, 1, 15))
                .amountUsd(new BigDecimal("150.50"))
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(exchangeRateClient.getExchangeRate(currencyCode, LocalDate.of(2024, 1, 15)))
                .thenThrow(new RuntimeException("Treasury API unavailable"));

        // Act & Assert
        CurrencyConversionException exception = assertThrows(CurrencyConversionException.class, () ->
                service.getTransactionWithConversion(transactionId, currencyCode)
        );
        assertTrue(exception.getMessage().contains("Treasury API unavailable"));
    }

    /**
     * Test: Currency conversion with different amounts and rates
     * Given: Various transaction amounts and exchange rates
     * When: Transactions are converted
     * Then: Correct converted amounts are calculated (rounded to 2 decimals)
     */
    @Test
    @DisplayName("Should correctly calculate converted amounts")
    void testCurrencyConversionCalculations() {
        // Test cases: (amountUsd, rate, expectedConverted)
        var testCases = new Object[][] {
                {new BigDecimal("100.00"), new BigDecimal("0.85"), new BigDecimal("85.00")},
                {new BigDecimal("150.50"), new BigDecimal("0.8124"), new BigDecimal("122.27")},
                {new BigDecimal("999.99"), new BigDecimal("1.25"), new BigDecimal("1249.99")},
                {new BigDecimal("0.01"), new BigDecimal("0.50"), new BigDecimal("0.01")}, // Rounding test
        };

        for (Object[] testCase : testCases) {
            BigDecimal amountUsd = (BigDecimal) testCase[0];
            BigDecimal rate = (BigDecimal) testCase[1];
            BigDecimal expected = (BigDecimal) testCase[2];

            String transactionId = "test-id";
            String currencyCode = "EUR";

            PurchaseTransaction transaction = PurchaseTransaction.builder()
                    .id(transactionId)
                    .description("Test")
                    .transactionDate(LocalDate.of(2024, 1, 15))
                    .amountUsd(amountUsd)
                    .build();

            ExchangeRate exchangeRate = ExchangeRate.builder()
                    .currencyCode(currencyCode)
                    .effectiveDate(LocalDate.of(2024, 1, 15))
                    .exchangeRate(rate)
                    .build();

            when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
            when(exchangeRateClient.getExchangeRate(currencyCode, LocalDate.of(2024, 1, 15)))
                    .thenReturn(exchangeRate);

            PurchaseTransactionResponse response = service.getTransactionWithConversion(transactionId, currencyCode);

            assertEquals(expected, response.getConvertedAmount(),
                    String.format("Failed for: %s * %s = %s", amountUsd, rate, expected));
        }
    }

    // ===== EDGE CASE TESTS: CURRENCY CODE VALIDATION =====

    @Test
    @DisplayName("Should reject null currency code")
    void testNullCurrencyCode() {
        String transactionId = "test-id";
        PurchaseTransaction transaction = PurchaseTransaction.builder()
                .id(transactionId)
                .description("Test")
                .transactionDate(LocalDate.of(2024, 1, 15))
                .amountUsd(new BigDecimal("100.00"))
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));

        assertThrows(IllegalArgumentException.class, () ->
                service.getTransactionWithConversion(transactionId, null)
        );
    }

    @Test
    @DisplayName("Should reject empty currency code")
    void testEmptyCurrencyCode() {
        String transactionId = "test-id";
        PurchaseTransaction transaction = PurchaseTransaction.builder()
                .id(transactionId)
                .description("Test")
                .transactionDate(LocalDate.of(2024, 1, 15))
                .amountUsd(new BigDecimal("100.00"))
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));

        assertThrows(IllegalArgumentException.class, () ->
                service.getTransactionWithConversion(transactionId, "")
        );
    }

    @Test
    @DisplayName("Should reject currency code with only whitespace")
    void testWhitespaceCurrencyCode() {
        String transactionId = "test-id";
        PurchaseTransaction transaction = PurchaseTransaction.builder()
                .id(transactionId)
                .description("Test")
                .transactionDate(LocalDate.of(2024, 1, 15))
                .amountUsd(new BigDecimal("100.00"))
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));

        assertThrows(IllegalArgumentException.class, () ->
                service.getTransactionWithConversion(transactionId, "   ")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"GB", "GBPX", "gb1", "G@P", "123"})
    @DisplayName("Should reject invalid currency code formats")
    void testInvalidCurrencyCodeFormats(String invalidCode) {
        String transactionId = "test-id";
        PurchaseTransaction transaction = PurchaseTransaction.builder()
                .id(transactionId)
                .description("Test")
                .transactionDate(LocalDate.of(2024, 1, 15))
                .amountUsd(new BigDecimal("100.00"))
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.getTransactionWithConversion(transactionId, invalidCode)
        );
        assertEquals("Invalid currency code format", ex.getMessage().substring(0, "Invalid currency code format".length()));
    }

    @Test
    @DisplayName("Should normalize lowercase currency code to uppercase")
    void testLowercaseCurrencyCodeNormalization() {
        String transactionId = "test-id";
        String currencyCode = "gbp";

        PurchaseTransaction transaction = PurchaseTransaction.builder()
                .id(transactionId)
                .description("Test")
                .transactionDate(LocalDate.of(2024, 1, 15))
                .amountUsd(new BigDecimal("100.00"))
                .build();

        ExchangeRate exchangeRate = ExchangeRate.builder()
                .currencyCode("GBP")
                .effectiveDate(LocalDate.of(2024, 1, 15))
                .exchangeRate(new BigDecimal("0.85"))
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(exchangeRateClient.getExchangeRate("GBP", LocalDate.of(2024, 1, 15)))
                .thenReturn(exchangeRate);

        PurchaseTransactionResponse response = service.getTransactionWithConversion(transactionId, currencyCode);

        assertNotNull(response);
        assertEquals("GBP", response.getCurrencyCode());
    }

    @Test
    @DisplayName("Should trim whitespace from currency code")
    void testCurrencyCodeTrimming() {
        String transactionId = "test-id";
        String currencyCode = "  GBP  ";

        PurchaseTransaction transaction = PurchaseTransaction.builder()
                .id(transactionId)
                .description("Test")
                .transactionDate(LocalDate.of(2024, 1, 15))
                .amountUsd(new BigDecimal("100.00"))
                .build();

        ExchangeRate exchangeRate = ExchangeRate.builder()
                .currencyCode("GBP")
                .effectiveDate(LocalDate.of(2024, 1, 15))
                .exchangeRate(new BigDecimal("0.85"))
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(exchangeRateClient.getExchangeRate("GBP", LocalDate.of(2024, 1, 15)))
                .thenReturn(exchangeRate);

        PurchaseTransactionResponse response = service.getTransactionWithConversion(transactionId, currencyCode);

        assertNotNull(response);
        assertEquals("GBP", response.getCurrencyCode());
    }

    // ===== EDGE CASE TESTS: EXCHANGE RATE VALIDATION =====

    @Test
    @DisplayName("Should reject null exchange rate object")
    void testNullExchangeRateObject() {
        String transactionId = "test-id";
        String currencyCode = "GBP";

        PurchaseTransaction transaction = PurchaseTransaction.builder()
                .id(transactionId)
                .description("Test")
                .transactionDate(LocalDate.of(2024, 1, 15))
                .amountUsd(new BigDecimal("100.00"))
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(exchangeRateClient.getExchangeRate(currencyCode, LocalDate.of(2024, 1, 15)))
                .thenReturn(null);

        CurrencyConversionException ex = assertThrows(CurrencyConversionException.class, () ->
                service.getTransactionWithConversion(transactionId, currencyCode)
        );
        assertEquals("Treasury API returned null exchange rate for GBP", ex.getMessage());
    }

    @Test
    @DisplayName("Should reject exchange rate with null value")
    void testNullExchangeRateValue() {
        String transactionId = "test-id";
        String currencyCode = "GBP";

        PurchaseTransaction transaction = PurchaseTransaction.builder()
                .id(transactionId)
                .description("Test")
                .transactionDate(LocalDate.of(2024, 1, 15))
                .amountUsd(new BigDecimal("100.00"))
                .build();

        ExchangeRate invalidRate = ExchangeRate.builder()
                .currencyCode("GBP")
                .effectiveDate(LocalDate.of(2024, 1, 15))
                .exchangeRate(null)  // NULL VALUE
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(exchangeRateClient.getExchangeRate(currencyCode, LocalDate.of(2024, 1, 15)))
                .thenReturn(invalidRate);

        CurrencyConversionException ex = assertThrows(CurrencyConversionException.class, () ->
                service.getTransactionWithConversion(transactionId, currencyCode)
        );
        assertEquals("Invalid exchange rate received for GBP: null", ex.getMessage());
    }

    @Test
    @DisplayName("Should reject zero exchange rate")
    void testZeroExchangeRate() {
        String transactionId = "test-id";
        String currencyCode = "GBP";

        PurchaseTransaction transaction = PurchaseTransaction.builder()
                .id(transactionId)
                .description("Test")
                .transactionDate(LocalDate.of(2024, 1, 15))
                .amountUsd(new BigDecimal("100.00"))
                .build();

        ExchangeRate zeroRate = ExchangeRate.builder()
                .currencyCode("GBP")
                .effectiveDate(LocalDate.of(2024, 1, 15))
                .exchangeRate(BigDecimal.ZERO)  // ZERO
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(exchangeRateClient.getExchangeRate(currencyCode, LocalDate.of(2024, 1, 15)))
                .thenReturn(zeroRate);

        CurrencyConversionException ex = assertThrows(CurrencyConversionException.class, () ->
                service.getTransactionWithConversion(transactionId, currencyCode)
        );
        assertEquals("Invalid exchange rate received for GBP: 0", ex.getMessage());
    }

    @Test
    @DisplayName("Should reject negative exchange rate")
    void testNegativeExchangeRate() {
        String transactionId = "test-id";
        String currencyCode = "GBP";

        PurchaseTransaction transaction = PurchaseTransaction.builder()
                .id(transactionId)
                .description("Test")
                .transactionDate(LocalDate.of(2024, 1, 15))
                .amountUsd(new BigDecimal("100.00"))
                .build();

        ExchangeRate negativeRate = ExchangeRate.builder()
                .currencyCode("GBP")
                .effectiveDate(LocalDate.of(2024, 1, 15))
                .exchangeRate(new BigDecimal("-0.85"))  // NEGATIVE
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(exchangeRateClient.getExchangeRate(currencyCode, LocalDate.of(2024, 1, 15)))
                .thenReturn(negativeRate);

        CurrencyConversionException ex = assertThrows(CurrencyConversionException.class, () ->
                service.getTransactionWithConversion(transactionId, currencyCode)
        );
        assertEquals("Invalid exchange rate received for GBP: -0.85", ex.getMessage());
    }

    // ===== EDGE CASE TESTS: AMOUNT PRECISION =====

    @Test
    @DisplayName("Should handle conversion with high precision rates")
    void testHighPrecisionExchangeRate() {
        String transactionId = "test-id";
        String currencyCode = "JPY";

        PurchaseTransaction transaction = PurchaseTransaction.builder()
                .id(transactionId)
                .description("Test")
                .transactionDate(LocalDate.of(2024, 1, 15))
                .amountUsd(new BigDecimal("100.00"))
                .build();

        ExchangeRate highPrecisionRate = ExchangeRate.builder()
                .currencyCode("JPY")
                .effectiveDate(LocalDate.of(2024, 1, 15))
                .exchangeRate(new BigDecimal("149.876543"))  // High precision
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(exchangeRateClient.getExchangeRate(currencyCode, LocalDate.of(2024, 1, 15)))
                .thenReturn(highPrecisionRate);

        PurchaseTransactionResponse response = service.getTransactionWithConversion(transactionId, currencyCode);

        assertNotNull(response);
        assertEquals(new BigDecimal("14987.65"), response.getConvertedAmount());
    }

    @Test
    @DisplayName("Should handle very small amounts with rounding")
    void testVerySmallAmountRounding() {
        String transactionId = "test-id";
        String currencyCode = "GBP";

        PurchaseTransaction transaction = PurchaseTransaction.builder()
                .id(transactionId)
                .description("Test")
                .transactionDate(LocalDate.of(2024, 1, 15))
                .amountUsd(new BigDecimal("0.01"))  // One cent
                .build();

        ExchangeRate exchangeRate = ExchangeRate.builder()
                .currencyCode("GBP")
                .effectiveDate(LocalDate.of(2024, 1, 15))
                .exchangeRate(new BigDecimal("0.7999"))  // High precision
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(exchangeRateClient.getExchangeRate(currencyCode, LocalDate.of(2024, 1, 15)))
                .thenReturn(exchangeRate);

        PurchaseTransactionResponse response = service.getTransactionWithConversion(transactionId, currencyCode);

        assertNotNull(response);
        assertEquals(new BigDecimal("0.01"), response.getConvertedAmount());
    }

    @Test
    @DisplayName("Should handle boundary amount: exactly 15 integer digits")
    void testMaxIntegerDigitAmount() {
        String transactionId = "test-id";
        String currencyCode = "EUR";

        PurchaseTransaction transaction = PurchaseTransaction.builder()
                .id(transactionId)
                .description("Test")
                .transactionDate(LocalDate.of(2024, 1, 15))
                .amountUsd(new BigDecimal("999999999999999.99"))  // 15 integer digits
                .build();

        ExchangeRate exchangeRate = ExchangeRate.builder()
                .currencyCode("EUR")
                .effectiveDate(LocalDate.of(2024, 1, 15))
                .exchangeRate(new BigDecimal("1.10"))
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(exchangeRateClient.getExchangeRate(currencyCode, LocalDate.of(2024, 1, 15)))
                .thenReturn(exchangeRate);

        PurchaseTransactionResponse response = service.getTransactionWithConversion(transactionId, currencyCode);

        assertNotNull(response);
        assertEquals(new BigDecimal("1099999999999999.99"), response.getConvertedAmount());
    }

    // ===== EDGE CASE TESTS: DATE BOUNDARIES =====

    @Test
    @DisplayName("Should handle transaction date of today")
    void testTransactionDateToday() {
        String transactionId = "test-id";
        String currencyCode = "GBP";
        LocalDate today = LocalDate.now();

        PurchaseTransaction transaction = PurchaseTransaction.builder()
                .id(transactionId)
                .description("Test")
                .transactionDate(today)
                .amountUsd(new BigDecimal("100.00"))
                .build();

        ExchangeRate exchangeRate = ExchangeRate.builder()
                .currencyCode("GBP")
                .effectiveDate(today)
                .exchangeRate(new BigDecimal("0.85"))
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(exchangeRateClient.getExchangeRate(currencyCode, today))
                .thenReturn(exchangeRate);

        PurchaseTransactionResponse response = service.getTransactionWithConversion(transactionId, currencyCode);

        assertNotNull(response);
        assertEquals(today, response.getTransactionDate());
    }

    // ===== EDGE CASE TESTS: ROUNDING PRECISION =====

    @ParameterizedTest
    @ValueSource(strings = {"0.005", "0.015", "0.025", "0.035", "0.045"})
    @DisplayName("Should apply HALF_UP rounding correctly for fractional cents")
    void testHalfUpRounding(String fractionToRound) {
        String transactionId = "test-id";
        String currencyCode = "EUR";

        PurchaseTransaction transaction = PurchaseTransaction.builder()
                .id(transactionId)
                .description("Test")
                .transactionDate(LocalDate.of(2024, 1, 15))
                .amountUsd(new BigDecimal("1.00"))
                .build();

        BigDecimal rate = new BigDecimal(fractionToRound);

        ExchangeRate exchangeRate = ExchangeRate.builder()
                .currencyCode("EUR")
                .effectiveDate(LocalDate.of(2024, 1, 15))
                .exchangeRate(rate)
                .build();

        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));
        when(exchangeRateClient.getExchangeRate(currencyCode, LocalDate.of(2024, 1, 15)))
                .thenReturn(exchangeRate);

        PurchaseTransactionResponse response = service.getTransactionWithConversion(transactionId, currencyCode);

        assertNotNull(response);
        assertEquals(2, response.getConvertedAmount().scale());
        assertEquals(0, response.getConvertedAmount().compareTo(
                new BigDecimal(fractionToRound).multiply(new BigDecimal("1.00"))
                        .setScale(2, java.math.RoundingMode.HALF_UP)
        ));
    }
}
