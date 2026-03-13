package com.loan.decision.outcomes;

import com.loan.decision.loanintake.model.LoanApplication;
import com.loan.decision.loanintake.repository.LoanApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Service for recording and managing loan outcomes.
 * Outcomes are used for ML model training and portfolio monitoring.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoanOutcomeService {

    private final LoanOutcomeRepository outcomeRepository;
    private final LoanApplicationRepository applicationRepository;

    /**
     * Records the outcome of a loan.
     *
     * @param applicationId the loan application ID
     * @param outcomeType the outcome type
     * @return the recorded outcome
     * @throws IllegalArgumentException if application not found
     * @throws IllegalStateException if outcome already recorded
     */
    @Transactional
    public LoanOutcome recordOutcome(UUID applicationId, LoanOutcome.OutcomeType outcomeType) {
        return recordOutcome(applicationId, outcomeType, null, null, null, null);
    }

    /**
     * Records the outcome of a loan with additional details.
     *
     * @param applicationId the loan application ID
     * @param outcomeType the outcome type
     * @param daysLate days late (for delinquent)
     * @param amountRecovered amount recovered (for default/write-off)
     * @param recoveryStatus recovery status
     * @param notes additional notes
     * @return the recorded outcome
     */
    @Transactional
    public LoanOutcome recordOutcome(UUID applicationId,
                                      LoanOutcome.OutcomeType outcomeType,
                                      Integer daysLate,
                                      BigDecimal amountRecovered,
                                      String recoveryStatus,
                                      String notes) {
        log.info("Recording outcome {} for application: {}", outcomeType, applicationId);

        // Validate application exists
        LoanApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Application not found: " + applicationId));

        // Check if outcome already recorded
        if (outcomeRepository.existsByLoanApplicationId(applicationId)) {
            throw new IllegalStateException(
                    "Outcome already recorded for application: " + applicationId);
        }

        // Create and persist outcome
        LoanOutcome outcome = LoanOutcome.builder()
                .loanApplication(application)
                .outcome(outcomeType)
                .daysLate(daysLate)
                .amountRecovered(amountRecovered)
                .recoveryStatus(recoveryStatus)
                .notes(notes)
                .build();

        LoanOutcome saved = outcomeRepository.save(outcome);
        log.info("Recorded outcome {} for application {} with ID {}",
                outcomeType, applicationId, saved.getId());

        return saved;
    }

    /**
     * Gets the outcome for a loan application.
     *
     * @param applicationId the loan application ID
     * @return the outcome
     * @throws IllegalArgumentException if outcome not found
     */
    @Transactional(readOnly = true)
    public LoanOutcome getOutcome(UUID applicationId) {
        return outcomeRepository.findByLoanApplicationId(applicationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Outcome not found for application: " + applicationId));
    }

    /**
     * Checks if an outcome has been recorded for an application.
     */
    @Transactional(readOnly = true)
    public boolean hasOutcome(UUID applicationId) {
        return outcomeRepository.existsByLoanApplicationId(applicationId);
    }

    /**
     * Gets all outcomes of a specific type.
     */
    @Transactional(readOnly = true)
    public List<LoanOutcome> getOutcomesByType(LoanOutcome.OutcomeType outcomeType) {
        return outcomeRepository.findByOutcome(outcomeType);
    }

    /**
     * Gets outcome statistics for reporting.
     */
    @Transactional(readOnly = true)
    public OutcomeStatistics getStatistics() {
        long total = outcomeRepository.count();
        long repaid = outcomeRepository.countByOutcome(LoanOutcome.OutcomeType.REPAID);
        long defaulted = outcomeRepository.countByOutcome(LoanOutcome.OutcomeType.DEFAULTED);
        long delinquent = outcomeRepository.countByOutcome(LoanOutcome.OutcomeType.DELINQUENT);
        long writtenOff = outcomeRepository.countByOutcome(LoanOutcome.OutcomeType.WRITTEN_OFF);
        long performing = outcomeRepository.countByOutcome(LoanOutcome.OutcomeType.PERFORMING);
        long prepaid = outcomeRepository.countByOutcome(LoanOutcome.OutcomeType.PREPAID);

        double defaultRate = total > 0 ? (double) (defaulted + writtenOff) / total : 0.0;

        return OutcomeStatistics.builder()
                .totalOutcomes(total)
                .repaid(repaid)
                .defaulted(defaulted)
                .delinquent(delinquent)
                .writtenOff(writtenOff)
                .performing(performing)
                .prepaid(prepaid)
                .defaultRate(defaultRate)
                .build();
    }

    /**
     * Updates an existing outcome (e.g., DELINQUENT -> DEFAULTED).
     */
    @Transactional
    public LoanOutcome updateOutcome(UUID applicationId,
                                      LoanOutcome.OutcomeType newOutcome,
                                      String notes) {
        LoanOutcome existing = outcomeRepository.findByLoanApplicationId(applicationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Outcome not found for application: " + applicationId));

        log.info("Updating outcome for application {} from {} to {}",
                applicationId, existing.getOutcome(), newOutcome);

        existing.setOutcome(newOutcome);
        if (notes != null) {
            existing.setNotes(notes);
        }

        return outcomeRepository.save(existing);
    }

    /**
     * Statistics about loan outcomes.
     */
    @lombok.Data
    @lombok.Builder
    public static class OutcomeStatistics {
        private long totalOutcomes;
        private long repaid;
        private long defaulted;
        private long delinquent;
        private long writtenOff;
        private long performing;
        private long prepaid;
        private double defaultRate;
    }
}
