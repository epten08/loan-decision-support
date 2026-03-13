package com.loan.decision.policy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service providing access to the active decision policy.
 * Acts as the main interface for other services to access policy configuration.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PolicyService {

    private final PolicyLoader policyLoader;

    /**
     * Returns the currently active decision policy.
     *
     * @return the current decision policy
     */
    public DecisionPolicy getCurrentPolicy() {
        return policyLoader.getCachedPolicy();
    }

    /**
     * Reloads the policy from configuration.
     * Can be called to pick up configuration changes without restarting.
     *
     * @return the reloaded policy
     */
    public DecisionPolicy reloadPolicy() {
        log.info("Policy reload requested");
        DecisionPolicy policy = policyLoader.reloadPolicy();
        log.info("Policy reloaded successfully - version: {}", policy.getMetadata().getVersion());
        return policy;
    }

    /**
     * Returns the policy version string.
     *
     * @return the policy version
     */
    public String getPolicyVersion() {
        return getCurrentPolicy().getMetadata().getVersion();
    }

    /**
     * Validates a loan amount against policy limits.
     *
     * @param amount the loan amount to validate
     * @return true if the amount is within policy limits
     */
    public boolean isAmountWithinLimits(java.math.BigDecimal amount) {
        DecisionPolicy policy = getCurrentPolicy();
        return amount.compareTo(policy.getLoanLimits().getMaxAmount()) <= 0;
    }

    /**
     * Validates income against minimum policy requirement.
     *
     * @param income the monthly income to validate
     * @return true if the income meets minimum requirements
     */
    public boolean isIncomeAboveMinimum(java.math.BigDecimal income) {
        DecisionPolicy policy = getCurrentPolicy();
        return income.compareTo(policy.getLoanLimits().getMinIncome()) >= 0;
    }

    /**
     * Validates loan term against policy limits.
     *
     * @param termMonths the loan term in months
     * @return true if the term is within policy limits
     */
    public boolean isTermWithinLimits(int termMonths) {
        DecisionPolicy policy = getCurrentPolicy();
        return termMonths >= policy.getLoanLimits().getMinTermMonths()
                && termMonths <= policy.getLoanLimits().getMaxTermMonths();
    }

    /**
     * Validates credit score against minimum policy requirement.
     *
     * @param creditScore the credit score to validate
     * @return true if the credit score meets minimum requirements
     */
    public boolean isCreditScoreAboveMinimum(int creditScore) {
        DecisionPolicy policy = getCurrentPolicy();
        return creditScore >= policy.getCreditRequirements().getMinCreditScore();
    }
}
