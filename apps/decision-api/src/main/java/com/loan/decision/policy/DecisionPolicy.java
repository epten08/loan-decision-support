package com.loan.decision.policy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Represents the complete decision policy configuration.
 * This model is populated from the decision-policy.yaml file.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecisionPolicy {

    private RiskThresholds riskThresholds;
    private RulesPolicy rules;
    private LoanLimits loanLimits;
    private CreditRequirements creditRequirements;
    private PolicyMetadata metadata;

    /**
     * Determines if the PD qualifies for automatic approval.
     */
    public boolean isAutoApprove(BigDecimal pd) {
        return pd.compareTo(riskThresholds.getApprove()) < 0;
    }

    /**
     * Determines if the PD qualifies for manual review (between approve and decline thresholds).
     */
    public boolean isManualReview(BigDecimal pd) {
        return pd.compareTo(riskThresholds.getApprove()) >= 0
                && pd.compareTo(riskThresholds.getReview()) < 0;
    }

    /**
     * Determines if the PD qualifies for automatic decline.
     */
    public boolean isAutoDecline(BigDecimal pd) {
        return pd.compareTo(riskThresholds.getReview()) >= 0;
    }

    /**
     * Gets the approve PD threshold.
     */
    public BigDecimal getApprovePdThreshold() {
        return riskThresholds.getApprove();
    }

    /**
     * Gets the review PD threshold (above which loans are declined).
     */
    public BigDecimal getReviewPdThreshold() {
        return riskThresholds.getReview();
    }

    /**
     * Gets the maximum number of soft failures allowed for auto-approval.
     */
    public int getMaxSoftFailuresForApproval() {
        return rules.getMaxSoftFailuresForApproval();
    }

    /**
     * Whether hard rule failures should result in automatic decline.
     */
    public boolean isHardFailDecline() {
        return rules.isHardFailDecline();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskThresholds {
        private BigDecimal approve;
        private BigDecimal review;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RulesPolicy {
        private boolean hardFailDecline;
        private int maxSoftFailuresForApproval;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoanLimits {
        private BigDecimal maxAmount;
        private BigDecimal minIncome;
        private int maxTermMonths;
        private int minTermMonths;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreditRequirements {
        private int minCreditScore;
        private BigDecimal maxDtiRatio;
        private int minCreditHistoryMonths;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PolicyMetadata {
        private String version;
        private String effectiveDate;
        private String description;
    }

    /**
     * Creates a default policy with standard values.
     * Used as fallback if configuration cannot be loaded.
     */
    public static DecisionPolicy defaultPolicy() {
        return DecisionPolicy.builder()
                .riskThresholds(RiskThresholds.builder()
                        .approve(BigDecimal.valueOf(0.05))
                        .review(BigDecimal.valueOf(0.15))
                        .build())
                .rules(RulesPolicy.builder()
                        .hardFailDecline(true)
                        .maxSoftFailuresForApproval(2)
                        .build())
                .loanLimits(LoanLimits.builder()
                        .maxAmount(BigDecimal.valueOf(50000))
                        .minIncome(BigDecimal.valueOf(1000))
                        .maxTermMonths(60)
                        .minTermMonths(6)
                        .build())
                .creditRequirements(CreditRequirements.builder()
                        .minCreditScore(600)
                        .maxDtiRatio(BigDecimal.valueOf(0.40))
                        .minCreditHistoryMonths(6)
                        .build())
                .metadata(PolicyMetadata.builder()
                        .version("default")
                        .effectiveDate("1970-01-01")
                        .description("Default fallback policy")
                        .build())
                .build();
    }
}
