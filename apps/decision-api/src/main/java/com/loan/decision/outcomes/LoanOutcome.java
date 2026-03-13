package com.loan.decision.outcomes;

import com.loan.decision.loanintake.model.LoanApplication;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tracks the actual outcome of a loan after disbursement.
 * This data is used for ML model training and validation.
 */
@Entity
@Table(name = "loan_outcomes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanOutcome {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_application_id", nullable = false, unique = true)
    private LoanApplication loanApplication;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutcomeType outcome;

    /**
     * Number of days the loan was late (for DELINQUENT outcomes).
     */
    private Integer daysLate;

    /**
     * Amount recovered (for DEFAULTED or WRITTEN_OFF outcomes).
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal amountRecovered;

    /**
     * Recovery status (for DEFAULTED or WRITTEN_OFF outcomes).
     */
    private String recoveryStatus;

    /**
     * Additional notes about the outcome.
     */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /**
     * When the outcome was recorded in the system.
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime recordedAt;

    /**
     * Who recorded the outcome.
     */
    private String recordedBy;

    @PrePersist
    protected void onCreate() {
        recordedAt = LocalDateTime.now();
        if (recordedBy == null) {
            recordedBy = "SYSTEM";
        }
    }

    /**
     * Possible loan outcomes after disbursement.
     */
    public enum OutcomeType {
        /**
         * Loan was fully repaid on time.
         */
        REPAID,

        /**
         * Borrower defaulted on the loan.
         */
        DEFAULTED,

        /**
         * Loan is currently delinquent (late payments).
         */
        DELINQUENT,

        /**
         * Loan was written off as a loss.
         */
        WRITTEN_OFF,

        /**
         * Loan is currently performing (on-time payments).
         */
        PERFORMING,

        /**
         * Loan was prepaid before maturity.
         */
        PREPAID
    }
}
