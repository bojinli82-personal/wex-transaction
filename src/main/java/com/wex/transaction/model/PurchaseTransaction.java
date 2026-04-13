package com.wex.transaction.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity class representing a purchase transaction
 * 
 * This entity stores information about a purchase transaction:
 * - Description: What was purchased (max 50 characters)
 * - Transaction Date: When the purchase occurred
 * - Amount: The purchase amount in US dollars (positive, rounded to nearest cent)
 * - Unique Identifier: Auto-generated UUID to uniquely identify the transaction
 * - Timestamps: For audit trail (creation and last modification times)
 * 
 * Validation constraints are applied at both the model and database levels
 * to ensure data integrity.
 * 
 * @author Bo
 */
@Entity
@Table(name = "purchase_transactions", indexes = {
    @Index(name = "idx_transaction_date", columnList = "transaction_date"),
    @Index(name = "idx_created_date", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseTransaction {
    
    /**
     * Unique identifier for the transaction
     * Auto-generated UUID to ensure uniqueness across the entire system
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private String id;

    /**
     * Description of the purchase
     * Constraints:
     * - Required (not null)
     * - Maximum 50 characters
     * - Must not be blank
     */
    @Column(name = "description", nullable = false, length = 50)
    @NotNull(message = "Description cannot be null")
    @NotBlank(message = "Description cannot be blank")
    @Size(max = 50, message = "Description must not exceed 50 characters")
    private String description;

    /**
     * The date when the purchase transaction occurred
     * Used to determine which exchange rate to apply for currency conversion
     * Constraints:
     * - Required (not null)
     * - Must not be in the future
     */
    @Column(name = "transaction_date", nullable = false)
    @NotNull(message = "Transaction date cannot be null")
    @PastOrPresent(message = "Transaction date cannot be in the future")
    private LocalDate transactionDate;

    /**
     * The purchase amount in US dollars
     * Constraints:
     * - Required (not null)
     * - Must be positive
     * - Rounded to nearest cent (2 decimal places)
     * - Stored as BIGINT cents in database for precision
     */
    @Column(name = "amount_usd", nullable = false, precision = 19, scale = 4)
    @NotNull(message = "Amount cannot be null")
    @DecimalMin(value = "0.01", message = "Amount must be a positive value")
    @Digits(integer = 15, fraction = 2, message = "Amount must have at most 15 integer digits and 2 decimal places")
    private BigDecimal amountUsd;

    /**
     * Timestamp when the transaction was created in the system
     * Set automatically when the record is first inserted
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the transaction was last updated
     * Set automatically on record creation and updates
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Lifecycle callback: Set creation timestamp before persisting
     * Called automatically by JPA before the first INSERT
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * Lifecycle callback: Update modification timestamp before updating
     * Called automatically by JPA before any UPDATE
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
