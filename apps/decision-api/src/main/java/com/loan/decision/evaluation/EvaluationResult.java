package com.loan.decision.evaluation;

import com.loan.decision.decisioning.model.Decision;
import com.loan.decision.riskadapter.model.RiskAssessment;
import com.loan.decision.rules.model.RuleResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Result object containing the complete evaluation outcome.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationResult {

    private UUID applicationId;
    private UUID decisionId;
    private Decision.DecisionOutcome outcome;
    private List<String> reasonCodes;
    private String riskBand;
    private BigDecimal probabilityOfDefault;
    private int hardRuleFailures;
    private int softRuleFailures;
    private String summary;
    private LocalDateTime evaluatedAt;

    /**
     * Flag indicating if the decision was retrieved from cache (already existed).
     */
    private boolean fromCache;

    /**
     * Full rule evaluation results for detailed analysis.
     */
    private List<RuleResult> ruleResults;

    /**
     * Risk assessment details.
     */
    private RiskAssessment riskAssessment;

    public static EvaluationResult fromDecision(Decision decision, boolean fromCache) {
        return EvaluationResult.builder()
                .applicationId(decision.getLoanApplication().getId())
                .decisionId(decision.getId())
                .outcome(decision.getOutcome())
                .reasonCodes(decision.getReasonCodes() != null && !decision.getReasonCodes().isEmpty()
                        ? List.of(decision.getReasonCodes().split(","))
                        : List.of())
                .riskBand(decision.getRiskBand())
                .probabilityOfDefault(decision.getProbabilityOfDefault())
                .hardRuleFailures(decision.getHardRuleFailures())
                .softRuleFailures(decision.getSoftRuleFailures())
                .summary(decision.getDecisionSummary())
                .evaluatedAt(decision.getDecidedAt())
                .fromCache(fromCache)
                .build();
    }

    public static EvaluationResult fromDecision(Decision decision,
                                                  List<RuleResult> ruleResults,
                                                  RiskAssessment riskAssessment) {
        return EvaluationResult.builder()
                .applicationId(decision.getLoanApplication().getId())
                .decisionId(decision.getId())
                .outcome(decision.getOutcome())
                .reasonCodes(decision.getReasonCodes() != null && !decision.getReasonCodes().isEmpty()
                        ? List.of(decision.getReasonCodes().split(","))
                        : List.of())
                .riskBand(decision.getRiskBand())
                .probabilityOfDefault(decision.getProbabilityOfDefault())
                .hardRuleFailures(decision.getHardRuleFailures())
                .softRuleFailures(decision.getSoftRuleFailures())
                .summary(decision.getDecisionSummary())
                .evaluatedAt(decision.getDecidedAt())
                .fromCache(false)
                .ruleResults(ruleResults)
                .riskAssessment(riskAssessment)
                .build();
    }
}
