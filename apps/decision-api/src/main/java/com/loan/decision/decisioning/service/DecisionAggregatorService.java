package com.loan.decision.decisioning.service;

import com.loan.decision.creditprofile.model.CreditProfile;
import com.loan.decision.creditprofile.repository.CreditProfileRepository;
import com.loan.decision.creditprofile.service.CreditProfileService;
import com.loan.decision.decisioning.controller.dto.DecisionResponse;
import com.loan.decision.decisioning.model.Decision;
import com.loan.decision.decisioning.repository.DecisionRepository;
import com.loan.decision.governance.model.DecisionAuditLog;
import com.loan.decision.governance.repository.DecisionAuditLogRepository;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DecisionAggregatorService {

    private final LoanApplicationRepository loanApplicationRepository;
    private final CreditProfileRepository creditProfileRepository;
    private final RuleEngineService ruleEngineService;
    private final RiskAdapterService riskAdapterService;
    private final DecisionRepository decisionRepository;
    private final DecisionAuditLogRepository auditLogRepository;

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

        // Aggregate decision
        Decision decision = aggregateDecision(application, ruleResults, riskAssessment);

        // Update application status
        application.setStatus(mapDecisionToStatus(decision.getOutcome()));
        loanApplicationRepository.save(application);

        // Create audit log
        createAuditLog(decision, ruleResults, riskAssessment);

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

    private Decision aggregateDecision(LoanApplication application,
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

        // Decision logic
        if (hardFailures > 0) {
            // Any HARD rule failure = DECLINE
            outcome = Decision.DecisionOutcome.DECLINED;
            summary = String.format("Declined due to %d hard rule failure(s)", hardFailures);
        } else {
            // No hard failures - check risk band
            RiskAssessment.RiskBand riskBand = riskAssessment.getRiskBand();

            switch (riskBand) {
                case A, B -> {
                    if (softFailures == 0) {
                        outcome = Decision.DecisionOutcome.APPROVED;
                        summary = String.format("Approved with risk band %s", riskBand);
                    } else {
                        outcome = Decision.DecisionOutcome.APPROVED;
                        summary = String.format("Approved with risk band %s (%d soft warnings)",
                                riskBand, softFailures);
                    }
                }
                case C -> {
                    outcome = Decision.DecisionOutcome.CONDITIONAL;
                    reasonCodes.add("MEDIUM_RISK_BAND");
                    summary = String.format("Conditional approval - risk band %s, requires additional review",
                            riskBand);
                }
                case D, E -> {
                    outcome = Decision.DecisionOutcome.DECLINED;
                    reasonCodes.add("HIGH_RISK_BAND");
                    summary = String.format("Declined due to high risk band %s (PD: %.2f%%)",
                            riskBand, riskAssessment.getProbabilityOfDefault()
                                    .multiply(BigDecimal.valueOf(100)));
                }
                default -> {
                    outcome = Decision.DecisionOutcome.PENDING_REVIEW;
                    summary = "Unable to determine outcome - pending manual review";
                }
            }
        }

        Decision decision = Decision.builder()
                .loanApplication(application)
                .outcome(outcome)
                .reasonCodes(String.join(",", reasonCodes))
                .riskBand(riskAssessment.getRiskBand().name())
                .hardRuleFailures((int) hardFailures)
                .softRuleFailures((int) softFailures)
                .decisionSummary(summary)
                .build();

        return decisionRepository.save(decision);
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

    private void createAuditLog(Decision decision,
                                List<RuleResult> ruleResults,
                                RiskAssessment riskAssessment) {
        String rulesSummary = ruleResults.stream()
                .map(r -> String.format("%s:%s", r.getRuleCode(), r.isPassed() ? "PASS" : "FAIL"))
                .collect(Collectors.joining(";"));

        DecisionAuditLog auditLog = DecisionAuditLog.builder()
                .decision(decision)
                .eventType(DecisionAuditLog.EventType.DECISION_MADE)
                .eventDescription("Automated decision evaluation completed")
                .previousState(null)
                .newState(decision.getOutcome().name())
                .ruleEvaluationSummary(rulesSummary)
                .riskAssessmentSummary(String.format("PD:%.4f;Band:%s;Confidence:%.2f",
                        riskAssessment.getProbabilityOfDefault(),
                        riskAssessment.getRiskBand(),
                        riskAssessment.getConfidence()))
                .build();

        auditLogRepository.save(auditLog);
    }

    private LoanApplication.ApplicationStatus mapDecisionToStatus(Decision.DecisionOutcome outcome) {
        return switch (outcome) {
            case APPROVED -> LoanApplication.ApplicationStatus.APPROVED;
            case DECLINED -> LoanApplication.ApplicationStatus.DECLINED;
            case CONDITIONAL -> LoanApplication.ApplicationStatus.CONDITIONAL;
            case PENDING_REVIEW -> LoanApplication.ApplicationStatus.UNDER_REVIEW;
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
                .hardRuleFailures(decision.getHardRuleFailures())
                .softRuleFailures(decision.getSoftRuleFailures())
                .summary(decision.getDecisionSummary())
                .decidedAt(decision.getDecidedAt())
                .build();
    }
}
