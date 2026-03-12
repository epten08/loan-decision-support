package com.loan.decision.decisioning.policy;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Encapsulates configurable policy thresholds for loan decisions.
 * These thresholds can be adjusted via application properties.
 */
@Component
@Getter
public class DecisionPolicy {

    /**
     * PD threshold below which loans are auto-approved (if no rule failures).
     * Default: 5% (0.05)
     */
    private final BigDecimal approvePdThreshold;

    /**
     * PD threshold below which loans go to manual review (if above approve threshold).
     * Loans with PD >= this threshold are declined.
     * Default: 15% (0.15)
     */
    private final BigDecimal reviewPdThreshold;

    /**
     * Maximum number of soft rule failures allowed for auto-approval.
     * Default: 2
     */
    private final int maxSoftFailuresForApproval;

    public DecisionPolicy(
            @Value("${decision.policy.approve-pd-threshold:0.05}") BigDecimal approvePdThreshold,
            @Value("${decision.policy.review-pd-threshold:0.15}") BigDecimal reviewPdThreshold,
            @Value("${decision.policy.max-soft-failures:2}") int maxSoftFailuresForApproval) {
        this.approvePdThreshold = approvePdThreshold;
        this.reviewPdThreshold = reviewPdThreshold;
        this.maxSoftFailuresForApproval = maxSoftFailuresForApproval;
    }

    /**
     * Determines if the PD qualifies for automatic approval.
     */
    public boolean isAutoApprove(BigDecimal pd) {
        return pd.compareTo(approvePdThreshold) < 0;
    }

    /**
     * Determines if the PD qualifies for manual review (between approve and decline thresholds).
     */
    public boolean isManualReview(BigDecimal pd) {
        return pd.compareTo(approvePdThreshold) >= 0 && pd.compareTo(reviewPdThreshold) < 0;
    }

    /**
     * Determines if the PD qualifies for automatic decline.
     */
    public boolean isAutoDecline(BigDecimal pd) {
        return pd.compareTo(reviewPdThreshold) >= 0;
    }
}
