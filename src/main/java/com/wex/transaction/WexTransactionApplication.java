package com.wex.transaction;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * WEX Transaction Management Application
 * 
 * This is the entry point for the Spring Boot application that manages purchase transactions
 * and provides currency conversion using the Treasury Reporting Rates of Exchange API.
 * 
 * The application provides RESTful endpoints to:
 * - Store purchase transactions with unique identifiers
 * - Retrieve transactions with currency conversion to any supported currency
 * 
 * Architecture:
 * - Controller layer: REST endpoints
 * - Service layer: Business logic including currency conversion
 * - Repository layer: Data persistence via JPA
 * - External API layer: Integration with Treasury API
 * 
 * @author Bo
 */
@SpringBootApplication
public class WexTransactionApplication {

    /**
     * Main entry point for the Spring Boot application
     * 
     * @param args Command line arguments passed to the application
     */
    public static void main(String[] args) {
        SpringApplication.run(WexTransactionApplication.class, args);
    }
}
