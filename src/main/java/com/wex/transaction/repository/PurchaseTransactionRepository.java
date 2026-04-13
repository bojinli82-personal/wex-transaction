package com.wex.transaction.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.wex.transaction.model.PurchaseTransaction;

/**
 * Repository interface for PurchaseTransaction entity
 * 
 * This interface provides database access methods for purchase transactions.
 * Spring Data JPA automatically implements this interface at runtime,
 * providing CRUD operations and custom query methods.
 * 
 * Extends JpaRepository to gain:
 * - save(), findById(), findAll(), delete(), etc.
 * - Pagination and sorting capabilities
 * - Custom query methods defined below
 */
@Repository
public interface PurchaseTransactionRepository extends JpaRepository<PurchaseTransaction, String> {

    /**
     * Find all transactions within a specific date range
     * 
     * @param startDate The start of the date range (inclusive)
     * @param endDate The end of the date range (inclusive)
     * @return List of transactions that occurred within the date range
     */
    List<PurchaseTransaction> findByTransactionDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Find all transactions for a specific date
     * 
     * @param transactionDate The date to search for
     * @return List of transactions that occurred on the specified date
     */
    List<PurchaseTransaction> findByTransactionDate(LocalDate transactionDate);

    /**
     * Find a transaction by its unique identifier
     * 
     * @param id The unique identifier
     * @return Optional containing the transaction if found
     */
    Optional<PurchaseTransaction> findById(String id);
}
