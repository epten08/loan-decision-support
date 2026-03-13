package com.loan.decision.policy;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class DecisionPolicyTest {

    private DecisionPolicy createTestPolicy(BigDecimal approveThreshold,
                                             BigDecimal reviewThreshold,
                                             int maxSoftFailures) {
        return DecisionPolicy.builder()
                .riskThresholds(DecisionPolicy.RiskThresholds.builder()
                        .approve(approveThreshold)
                        .review(reviewThreshold)
                        .build())
                .rules(DecisionPolicy.RulesPolicy.builder()
                        .hardFailDecline(true)
                        .maxSoftFailuresForApproval(maxSoftFailures)
                        .build())
                .loanLimits(DecisionPolicy.LoanLimits.builder()
                        .maxAmount(BigDecimal.valueOf(50000))
                        .minIncome(BigDecimal.valueOf(1000))
                        .maxTermMonths(60)
                        .minTermMonths(6)
                        .build())
                .creditRequirements(DecisionPolicy.CreditRequirements.builder()
                        .minCreditScore(600)
                        .maxDtiRatio(BigDecimal.valueOf(0.40))
                        .minCreditHistoryMonths(6)
                        .build())
                .metadata(DecisionPolicy.PolicyMetadata.builder()
                        .version("test-1.0")
                        .effectiveDate("2024-01-01")
                        .description("Test policy")
                        .build())
                .build();
    }

    @Test
    void isAutoApprove_belowThreshold_returnsTrue() {
        DecisionPolicy policy = createTestPolicy(
                BigDecimal.valueOf(0.05),
                BigDecimal.valueOf(0.15),
                2
        );

        assertTrue(policy.isAutoApprove(BigDecimal.valueOf(0.03)));
        assertTrue(policy.isAutoApprove(BigDecimal.valueOf(0.049)));
    }

    @Test
    void isAutoApprove_atOrAboveThreshold_returnsFalse() {
        DecisionPolicy policy = createTestPolicy(
                BigDecimal.valueOf(0.05),
                BigDecimal.valueOf(0.15),
                2
        );

        assertFalse(policy.isAutoApprove(BigDecimal.valueOf(0.05)));
        assertFalse(policy.isAutoApprove(BigDecimal.valueOf(0.10)));
    }

    @Test
    void isManualReview_betweenThresholds_returnsTrue() {
        DecisionPolicy policy = createTestPolicy(
                BigDecimal.valueOf(0.05),
                BigDecimal.valueOf(0.15),
                2
        );

        assertTrue(policy.isManualReview(BigDecimal.valueOf(0.05)));
        assertTrue(policy.isManualReview(BigDecimal.valueOf(0.10)));
        assertTrue(policy.isManualReview(BigDecimal.valueOf(0.149)));
    }

    @Test
    void isManualReview_outsideThresholds_returnsFalse() {
        DecisionPolicy policy = createTestPolicy(
                BigDecimal.valueOf(0.05),
                BigDecimal.valueOf(0.15),
                2
        );

        assertFalse(policy.isManualReview(BigDecimal.valueOf(0.03)));
        assertFalse(policy.isManualReview(BigDecimal.valueOf(0.15)));
        assertFalse(policy.isManualReview(BigDecimal.valueOf(0.20)));
    }

    @Test
    void isAutoDecline_atOrAboveThreshold_returnsTrue() {
        DecisionPolicy policy = createTestPolicy(
                BigDecimal.valueOf(0.05),
                BigDecimal.valueOf(0.15),
                2
        );

        assertTrue(policy.isAutoDecline(BigDecimal.valueOf(0.15)));
        assertTrue(policy.isAutoDecline(BigDecimal.valueOf(0.20)));
        assertTrue(policy.isAutoDecline(BigDecimal.valueOf(0.50)));
    }

    @Test
    void isAutoDecline_belowThreshold_returnsFalse() {
        DecisionPolicy policy = createTestPolicy(
                BigDecimal.valueOf(0.05),
                BigDecimal.valueOf(0.15),
                2
        );

        assertFalse(policy.isAutoDecline(BigDecimal.valueOf(0.03)));
        assertFalse(policy.isAutoDecline(BigDecimal.valueOf(0.10)));
        assertFalse(policy.isAutoDecline(BigDecimal.valueOf(0.149)));
    }

    @Test
    void getThresholds_returnCorrectValues() {
        DecisionPolicy policy = createTestPolicy(
                BigDecimal.valueOf(0.05),
                BigDecimal.valueOf(0.15),
                2
        );

        assertEquals(BigDecimal.valueOf(0.05), policy.getApprovePdThreshold());
        assertEquals(BigDecimal.valueOf(0.15), policy.getReviewPdThreshold());
        assertEquals(2, policy.getMaxSoftFailuresForApproval());
    }

    @Test
    void customThresholds_workCorrectly() {
        // More aggressive thresholds
        DecisionPolicy policy = createTestPolicy(
                BigDecimal.valueOf(0.03),
                BigDecimal.valueOf(0.10),
                1
        );

        // 4% PD - would be approved with default, but manual review with custom
        assertTrue(policy.isManualReview(BigDecimal.valueOf(0.04)));

        // 12% PD - would be manual review with default, but declined with custom
        assertTrue(policy.isAutoDecline(BigDecimal.valueOf(0.12)));
    }

    @Test
    void defaultPolicy_hasCorrectValues() {
        DecisionPolicy policy = DecisionPolicy.defaultPolicy();

        assertEquals(BigDecimal.valueOf(0.05), policy.getApprovePdThreshold());
        assertEquals(BigDecimal.valueOf(0.15), policy.getReviewPdThreshold());
        assertEquals(2, policy.getMaxSoftFailuresForApproval());
        assertTrue(policy.isHardFailDecline());
        assertEquals("default", policy.getMetadata().getVersion());
    }

    @Test
    void loanLimits_areAccessible() {
        DecisionPolicy policy = createTestPolicy(
                BigDecimal.valueOf(0.05),
                BigDecimal.valueOf(0.15),
                2
        );

        assertEquals(BigDecimal.valueOf(50000), policy.getLoanLimits().getMaxAmount());
        assertEquals(BigDecimal.valueOf(1000), policy.getLoanLimits().getMinIncome());
        assertEquals(60, policy.getLoanLimits().getMaxTermMonths());
        assertEquals(6, policy.getLoanLimits().getMinTermMonths());
    }

    @Test
    void creditRequirements_areAccessible() {
        DecisionPolicy policy = createTestPolicy(
                BigDecimal.valueOf(0.05),
                BigDecimal.valueOf(0.15),
                2
        );

        assertEquals(600, policy.getCreditRequirements().getMinCreditScore());
        assertEquals(BigDecimal.valueOf(0.40), policy.getCreditRequirements().getMaxDtiRatio());
        assertEquals(6, policy.getCreditRequirements().getMinCreditHistoryMonths());
    }
}
