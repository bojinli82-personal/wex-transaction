package com.wex.transaction.exception;

/**
 * Exception thrown when currency conversion cannot be performed
 * 
 * This occurs when:
 * - The requested currency is not supported by the Treasury API
 * - No exchange rate is available within 6 months of the purchase date
 * - The Treasury API is permanently unavailable or returns an error
 * 
 * This provides clear feedback to the client about why the conversion failed.
 */
public class CurrencyConversionException extends RuntimeException {

    public CurrencyConversionException(String message) {
        super(message);
    }

    public CurrencyConversionException(String message, Throwable cause) {
        super(message, cause);
    }
}
