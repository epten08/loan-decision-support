package com.loan.decision.evaluation;

import com.loan.decision.creditprofile.model.CreditProfile;
import com.loan.decision.creditprofile.repository.CreditProfileRepository;
import com.loan.decision.decisioning.model.Decision;
import com.loan.decision.decisioning.repository.DecisionRepository;
import com.loan.decision.decisioning.service.DecisionAggregatorService;
import com.loan.decision.loanintake.model.LoanApplication;
import com.loan.decision.loanintake.repository.LoanApplicationRepository;
import com.loan.decision.riskadapter.model.RiskAssessment;
import com.loan.decision.riskadapter.service.RiskAdapterService;
import com.loan.decision.rules.model.RuleResult;
import com.loan.decision.rules.service.RuleEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates the complete loan evaluation pipeline.
 *
 * This service coordinates:
 * 1. Loading the application
 * 2. Running the rule engine
 * 3. Calling the risk engine (if no hard failures)
 * 4. Aggregating the decision
 * 5. Persisting the decision and audit trail
 *
 * Controllers should only call this orchestrator, never individual services.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EvaluationOrchestrator {

    private final LoanApplicationRepository loanApplicationRepository;
    private final CreditProfileRepository creditProfileRepository;
    private final DecisionRepository decisionRepository;
    private final RuleEngineService ruleEngineService;
    private final RiskAdapterService riskAdapterService;
    private final DecisionAggregatorService decisionAggregatorService;

    /**
     * Evaluates a loan application through the complete pipeline.
     *
     * @param applicationId the application to evaluate
     * @return the evaluation result
     */
    @Transactional
    public EvaluationResult evaluate(UUID applicationId) {
        return evaluate(EvaluationRequest.of(applicationId));
    }

    /**
     * Evaluates a loan application through the complete pipeline.
     *
     * @param request the evaluation request
     * @return the evaluation result
     */
    @Transactional
    public EvaluationResult evaluate(EvaluationRequest request) {
        UUID applicationId = request.getApplicationId();
        log.info("Starting evaluation pipeline for application: {}", applicationId);

        // Step 1: Check if decision already exists (unless force re-evaluation)
        if (!request.isForceReevaluation() && decisionRepository.existsByLoanApplicationId(applicationId)) {
            log.info("Decision already exists for application: {}", applicationId);
            Decision existingDecision = decisionRepository.findByLoanApplicationId(applicationId)
                    .orElseThrow();
            return EvaluationResult.fromDecision(existingDecision, true);
        }

        // Step 2: Load the application
        LoanApplication application = loadApplication(applicationId);

        // Step 3: Update status to under review
        updateApplicationStatus(application, LoanApplication.ApplicationStatus.UNDER_REVIEW);

        // Step 4: Get or create credit profile
        CreditProfile creditProfile = getOrCreateCreditProfile(application);

        // Step 5: Run rule engine
        log.info("Executing rule engine for application: {}", applicationId);
        List<RuleResult> ruleResults = ruleEngineService.evaluateApplication(application, creditProfile);

        // Step 6: Check for hard failures and conditionally call risk engine
        RiskAssessment riskAssessment;
        if (ruleEngineService.hasHardFailure(ruleResults)) {
            log.info("Hard rule failure detected - skipping risk engine");
            riskAssessment = createDeclinedRiskAssessment();
        } else {
            log.info("Calling risk engine for application: {}", applicationId);
            riskAssessment = riskAdapterService.assessRisk(application, creditProfile);
        }

        // Step 7: Aggregate decision (includes persistence and audit logging)
        log.info("Aggregating decision for application: {}", applicationId);
        Decision decision = decisionAggregatorService.aggregateDecision(
                application,
                ruleResults,
                riskAssessment
        );

        // Step 8: Update application status based on decision
        updateApplicationStatus(application, mapDecisionToStatus(decision.getOutcome()));

        log.info("Evaluation complete for application {}: {}", applicationId, decision.getOutcome());

        return EvaluationResult.fromDecision(decision, ruleResults, riskAssessment);
    }

    /**
     * Retrieves an existing decision without re-evaluating.
     */
    @Transactional(readOnly = true)
    public EvaluationResult getDecision(UUID applicationId) {
        Decision decision = decisionRepository.findByLoanApplicationId(applicationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Decision not found for application: " + applicationId));
        return EvaluationResult.fromDecision(decision, true);
    }

    private LoanApplication loadApplication(UUID applicationId) {
        return loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Application not found: " + applicationId));
    }

    private void updateApplicationStatus(LoanApplication application,
                                          LoanApplication.ApplicationStatus status) {
        application.setStatus(status);
        loanApplicationRepository.save(application);
    }

    private CreditProfile getOrCreateCreditProfile(LoanApplication application) {
        return creditProfileRepository.findByLoanApplicationId(application.getId())
                .orElseGet(() -> createDefaultCreditProfile(application));
    }

    private CreditProfile createDefaultCreditProfile(LoanApplication application) {
        CreditProfile profile = CreditProfile.builder()
                .loanApplication(application)
                .creditScore(650)
                .existingLoanCount(1)
                .totalExistingDebt(BigDecimal.valueOf(5000))
                .monthlyDebtPayments(BigDecimal.valueOf(200))
                .creditHistoryMonths(36)
                .missedPaymentsLast12Months(0)
                .defaultsLast5Years(0)
                .debtToIncomeRatio(application.getMonthlyIncome() != null ?
                        BigDecimal.valueOf(200).divide(application.getMonthlyIncome(),
                                        4, java.math.RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100)) :
                        BigDecimal.valueOf(20))
                .build();

        return creditProfileRepository.save(profile);
    }

    private RiskAssessment createDeclinedRiskAssessment() {
        return RiskAssessment.builder()
                .probabilityOfDefault(BigDecimal.ONE)
                .riskBand(RiskAssessment.RiskBand.E)
                .confidence(BigDecimal.ONE)
                .build();
    }

    private LoanApplication.ApplicationStatus mapDecisionToStatus(Decision.DecisionOutcome outcome) {
        return switch (outcome) {
            case APPROVED -> LoanApplication.ApplicationStatus.APPROVED;
            case DECLINED -> LoanApplication.ApplicationStatus.DECLINED;
            case CONDITIONAL -> LoanApplication.ApplicationStatus.CONDITIONAL;
            case MANUAL_REVIEW, PENDING_REVIEW -> LoanApplication.ApplicationStatus.UNDER_REVIEW;
        };
    }
}
