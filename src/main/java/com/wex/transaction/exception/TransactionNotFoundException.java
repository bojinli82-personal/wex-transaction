package com.wex.transaction.exception;

/**
 * Exception thrown when a purchase transaction is not found in the system
 * 
 * This typically occurs when attempting to retrieve a transaction by ID
 * that does not exist in the database.
 */
public class TransactionNotFoundException extends RuntimeException {

    public TransactionNotFoundException(String message) {
        super(message);
    }

    public TransactionNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
