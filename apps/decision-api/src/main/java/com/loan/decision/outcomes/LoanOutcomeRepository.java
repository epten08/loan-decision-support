package com.loan.decision.outcomes;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for loan outcome persistence.
 */
@Repository
public interface LoanOutcomeRepository extends JpaRepository<LoanOutcome, UUID> {

    /**
     * Find outcome by loan application ID.
     */
    Optional<LoanOutcome> findByLoanApplicationId(UUID applicationId);

    /**
     * Check if an outcome exists for an application.
     */
    boolean existsByLoanApplicationId(UUID applicationId);

    /**
     * Find all outcomes by outcome type.
     */
    List<LoanOutcome> findByOutcome(LoanOutcome.OutcomeType outcome);

    /**
     * Find outcomes recorded within a date range.
     */
    List<LoanOutcome> findByRecordedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Count outcomes by type.
     */
    long countByOutcome(LoanOutcome.OutcomeType outcome);

    /**
     * Find all defaulted loans for model training.
     */
    @Query("SELECT o FROM LoanOutcome o WHERE o.outcome = 'DEFAULTED' OR o.outcome = 'WRITTEN_OFF'")
    List<LoanOutcome> findAllNegativeOutcomes();

    /**
     * Find all repaid loans for model training.
     */
    @Query("SELECT o FROM LoanOutcome o WHERE o.outcome = 'REPAID' OR o.outcome = 'PREPAID'")
    List<LoanOutcome> findAllPositiveOutcomes();
}
