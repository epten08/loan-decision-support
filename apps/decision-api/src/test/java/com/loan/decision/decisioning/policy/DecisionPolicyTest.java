package com.loan.decision.decisioning.policy;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class DecisionPolicyTest {

    @Test
    void isAutoApprove_belowThreshold_returnsTrue() {
        DecisionPolicy policy = new DecisionPolicy(
                BigDecimal.valueOf(0.05),
                BigDecimal.valueOf(0.15),
                2
        );

        assertTrue(policy.isAutoApprove(BigDecimal.valueOf(0.03)));
        assertTrue(policy.isAutoApprove(BigDecimal.valueOf(0.049)));
    }

    @Test
    void isAutoApprove_atOrAboveThreshold_returnsFalse() {
        DecisionPolicy policy = new DecisionPolicy(
                BigDecimal.valueOf(0.05),
                BigDecimal.valueOf(0.15),
                2
        );

        assertFalse(policy.isAutoApprove(BigDecimal.valueOf(0.05)));
        assertFalse(policy.isAutoApprove(BigDecimal.valueOf(0.10)));
    }

    @Test
    void isManualReview_betweenThresholds_returnsTrue() {
        DecisionPolicy policy = new DecisionPolicy(
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
        DecisionPolicy policy = new DecisionPolicy(
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
        DecisionPolicy policy = new DecisionPolicy(
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
        DecisionPolicy policy = new DecisionPolicy(
                BigDecimal.valueOf(0.05),
                BigDecimal.valueOf(0.15),
                2
        );

        assertFalse(policy.isAutoDecline(BigDecimal.valueOf(0.03)));
        assertFalse(policy.isAutoDecline(BigDecimal.valueOf(0.10)));
        assertFalse(policy.isAutoDecline(BigDecimal.valueOf(0.149)));
    }

    @Test
    void defaultValues_areCorrect() {
        DecisionPolicy policy = new DecisionPolicy(
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
        DecisionPolicy policy = new DecisionPolicy(
                BigDecimal.valueOf(0.03),
                BigDecimal.valueOf(0.10),
                1
        );

        // 4% PD - would be approved with default, but manual review with custom
        assertTrue(policy.isManualReview(BigDecimal.valueOf(0.04)));

        // 12% PD - would be manual review with default, but declined with custom
        assertTrue(policy.isAutoDecline(BigDecimal.valueOf(0.12)));
    }
}
