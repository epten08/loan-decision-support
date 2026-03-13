package com.loan.decision.policy;

import lombok.Data;

import java.math.BigDecimal;

/**
 * YAML mapping model for the decision-policy.yaml file.
 * Uses snake_case property names to match the YAML structure.
 */
@Data
public class PolicyYamlModel {

    private RiskThresholds riskThresholds;
    private Rules rules;
    private LoanLimits loanLimits;
    private CreditRequirements creditRequirements;
    private Metadata metadata;

    @Data
    public static class RiskThresholds {
        private BigDecimal approve;
        private BigDecimal review;
    }

    @Data
    public static class Rules {
        private boolean hardFailDecline;
        private int maxSoftFailuresForApproval;
    }

    @Data
    public static class LoanLimits {
        private BigDecimal maxAmount;
        private BigDecimal minIncome;
        private int maxTermMonths;
        private int minTermMonths;
    }

    @Data
    public static class CreditRequirements {
        private int minCreditScore;
        private BigDecimal maxDtiRatio;
        private int minCreditHistoryMonths;
    }

    @Data
    public static class Metadata {
        private String version;
        private String effectiveDate;
        private String description;
    }
}
