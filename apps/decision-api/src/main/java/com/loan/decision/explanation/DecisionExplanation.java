package com.loan.decision.explanation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Provides a structured explanation of a loan decision.
 * This model combines data from decisions, audit logs, and policy
 * to provide a complete picture of why a decision was made.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecisionExplanation {

    private UUID applicationId;
    private String decision;
    private String decisionReason;
    private RiskAssessmentExplanation riskAssessment;
    private List<RuleResultExplanation> rules;
    private PolicySnapshot policy;
    private LocalDateTime timestamp;
    private String decidedBy;

    /**
     * Risk assessment details for explanation.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskAssessmentExplanation {
        private BigDecimal probabilityOfDefault;
        private String riskBand;
        private BigDecimal confidence;
        private String interpretation;
    }

    /**
     * Individual rule result for explanation.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RuleResultExplanation {
        private String rule;
        private String result;
        private String severity;
        private String description;
        private String reason;
    }

    /**
     * Policy snapshot at time of decision.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PolicySnapshot {
        private BigDecimal approveThreshold;
        private BigDecimal reviewThreshold;
        private int maxSoftFailuresForApproval;
        private boolean hardFailDecline;
        private String policyVersion;
    }

    /**
     * Summary statistics for the explanation.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DecisionSummary {
        private int totalRulesEvaluated;
        private int rulesPassed;
        private int rulesFailed;
        private int hardFailures;
        private int softFailures;
    }
}
