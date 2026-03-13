package com.loan.decision.decisioning.service;

import com.loan.decision.creditprofile.model.CreditProfile;
import com.loan.decision.creditprofile.repository.CreditProfileRepository;
import com.loan.decision.decisioning.controller.dto.DecisionResponse;
import com.loan.decision.decisioning.model.Decision;
import com.loan.decision.decisioning.policy.DecisionPolicy;
import com.loan.decision.decisioning.repository.DecisionRepository;
import com.loan.decision.governance.service.DecisionPersistenceService;
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
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DecisionAggregatorService {

    private final LoanApplicationRepository loanApplicationRepository;
    private final CreditProfileRepository creditProfileRepository;
    private final RuleEngineService ruleEngineService;
    private final RiskAdapterService riskAdapterService;
    private final DecisionRepository decisionRepository;
    private final DecisionPersistenceService decisionPersistenceService;
    private final DecisionPolicy decisionPolicy;

    /**
     * @deprecated Use {@link com.loan.decision.evaluation.EvaluationOrchestrator#evaluate(UUID)} instead.
     * This method is kept for backward compatibility.
     */
    @Deprecated
    @Transactional
    public DecisionResponse evaluateApplication(UUID applicationId) {
        log.info("Starting decision evaluation for application: {}", applicationId);

        // Check if decision already exists
        if (decisionRepository.existsByLoanApplicationId(applicationId)) {
            log.info("Decision already exists for application: {}", applicationId);
            return getDecision(applicationId);
        }

        LoanApplication application = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));

        // Update status to under review
        application.setStatus(LoanApplication.ApplicationStatus.UNDER_REVIEW);
        loanApplicationRepository.save(application);

        // Get or create credit profile (in real system, this would come from external source)
        CreditProfile creditProfile = creditProfileRepository.findByLoanApplicationId(applicationId)
                .orElseGet(() -> createDefaultCreditProfile(application));

        // Evaluate rules
        List<RuleResult> ruleResults = ruleEngineService.evaluateApplication(application, creditProfile);

        // Check for hard rule failures - skip risk engine if any hard failures
        RiskAssessment riskAssessment;
        if (ruleEngineService.hasHardFailure(ruleResults)) {
            log.info("Hard rule failure detected - skipping risk engine evaluation");
            riskAssessment = RiskAssessment.builder()
                    .probabilityOfDefault(BigDecimal.ONE)
                    .riskBand(RiskAssessment.RiskBand.E)
                    .confidence(BigDecimal.ONE)
                    .build();
        } else {
            // Get risk assessment only if no hard failures
            riskAssessment = riskAdapterService.assessRisk(application, creditProfile);
        }

        // Aggregate and persist decision with full audit trail
        Decision decision = aggregateDecision(application, ruleResults, riskAssessment);

        // Update application status
        application.setStatus(mapDecisionToStatus(decision.getOutcome()));
        loanApplicationRepository.save(application);

        log.info("Decision for application {}: {}", applicationId, decision.getOutcome());

        return mapToResponse(decision);
    }

    @Transactional(readOnly = true)
    public DecisionResponse getDecision(UUID applicationId) {
        Decision decision = decisionRepository.findByLoanApplicationId(applicationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Decision not found for application: " + applicationId));
        return mapToResponse(decision);
    }

    /**
     * Aggregates rule results and risk assessment into a final decision.
     * This method is called by the EvaluationOrchestrator.
     *
     * @param application the loan application
     * @param ruleResults the rule evaluation results
     * @param riskAssessment the risk assessment
     * @return the persisted decision
     */
    @Transactional
    public Decision aggregateDecision(LoanApplication application,
                                       List<RuleResult> ruleResults,
                                       RiskAssessment riskAssessment) {

        List<String> reasonCodes = new ArrayList<>();
        long hardFailures = ruleResults.stream().filter(RuleResult::isHardFail).count();
        long softFailures = ruleResults.stream()
                .filter(r -> r.isFailed() && !r.isHardFail()).count();

        // Collect reason codes from failed rules
        ruleResults.stream()
                .filter(RuleResult::isFailed)
                .forEach(r -> reasonCodes.add(r.getReason()));

        Decision.DecisionOutcome outcome;
        String summary;
        BigDecimal pd = riskAssessment.getProbabilityOfDefault();

        // Decision logic using policy thresholds
        if (hardFailures > 0) {
            // Any HARD rule failure = DECLINE
            outcome = Decision.DecisionOutcome.DECLINED;
            summary = String.format("Declined due to %d hard rule failure(s)", hardFailures);
        } else if (decisionPolicy.isAutoApprove(pd)) {
            // PD below approve threshold - auto approve if soft failures within limit
            if (softFailures <= decisionPolicy.getMaxSoftFailuresForApproval()) {
                outcome = Decision.DecisionOutcome.APPROVED;
                summary = String.format("Approved - PD %.2f%% below threshold (band %s)",
                        pd.multiply(BigDecimal.valueOf(100)), riskAssessment.getRiskBand());
            } else {
                outcome = Decision.DecisionOutcome.MANUAL_REVIEW;
                reasonCodes.add("SOFT_FAILURES_EXCEEDED");
                summary = String.format("Manual review required - %d soft failures exceed limit",
                        softFailures);
            }
        } else if (decisionPolicy.isManualReview(pd)) {
            // PD between approve and decline threshold - manual review
            outcome = Decision.DecisionOutcome.MANUAL_REVIEW;
            reasonCodes.add("PD_REQUIRES_REVIEW");
            summary = String.format("Manual review required - PD %.2f%% (band %s)",
                    pd.multiply(BigDecimal.valueOf(100)), riskAssessment.getRiskBand());
        } else {
            // PD above decline threshold - auto decline
            outcome = Decision.DecisionOutcome.DECLINED;
            reasonCodes.add("HIGH_PROBABILITY_OF_DEFAULT");
            summary = String.format("Declined - PD %.2f%% exceeds threshold (band %s)",
                    pd.multiply(BigDecimal.valueOf(100)), riskAssessment.getRiskBand());
        }

        // Use DecisionPersistenceService to save decision with full audit trail
        return decisionPersistenceService.saveDecision(
                application,
                outcome,
                ruleResults,
                riskAssessment,
                reasonCodes,
                summary
        );
    }

    private CreditProfile createDefaultCreditProfile(LoanApplication application) {
        // In a real system, this would fetch from a credit bureau
        // For now, create a default profile for testing
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

    private LoanApplication.ApplicationStatus mapDecisionToStatus(Decision.DecisionOutcome outcome) {
        return switch (outcome) {
            case APPROVED -> LoanApplication.ApplicationStatus.APPROVED;
            case DECLINED -> LoanApplication.ApplicationStatus.DECLINED;
            case CONDITIONAL -> LoanApplication.ApplicationStatus.CONDITIONAL;
            case MANUAL_REVIEW, PENDING_REVIEW -> LoanApplication.ApplicationStatus.UNDER_REVIEW;
        };
    }

    private DecisionResponse mapToResponse(Decision decision) {
        List<String> reasonCodes = decision.getReasonCodes() != null && !decision.getReasonCodes().isEmpty() ?
                Arrays.asList(decision.getReasonCodes().split(",")) :
                Collections.emptyList();

        return DecisionResponse.builder()
                .applicationId(decision.getLoanApplication().getId())
                .outcome(decision.getOutcome().name())
                .reasonCodes(reasonCodes)
                .riskBand(decision.getRiskBand())
                .probabilityOfDefault(decision.getProbabilityOfDefault())
                .hardRuleFailures(decision.getHardRuleFailures())
                .softRuleFailures(decision.getSoftRuleFailures())
                .summary(decision.getDecisionSummary())
                .decidedAt(decision.getDecidedAt())
                .build();
    }
}
