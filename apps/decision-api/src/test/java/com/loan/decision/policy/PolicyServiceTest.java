package com.loan.decision.policy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

    @Mock
    private PolicyLoader policyLoader;

    private PolicyService policyService;

    @BeforeEach
    void setUp() {
        policyService = new PolicyService(policyLoader);
    }

    @Test
    void getCurrentPolicy_delegatesToLoader() {
        DecisionPolicy mockPolicy = DecisionPolicy.defaultPolicy();
        when(policyLoader.getCachedPolicy()).thenReturn(mockPolicy);

        DecisionPolicy result = policyService.getCurrentPolicy();

        assertSame(mockPolicy, result);
        verify(policyLoader).getCachedPolicy();
    }

    @Test
    void reloadPolicy_delegatesToLoader() {
        DecisionPolicy mockPolicy = DecisionPolicy.defaultPolicy();
        when(policyLoader.reloadPolicy()).thenReturn(mockPolicy);

        DecisionPolicy result = policyService.reloadPolicy();

        assertSame(mockPolicy, result);
        verify(policyLoader).reloadPolicy();
    }

    @Test
    void getPolicyVersion_returnsVersion() {
        DecisionPolicy mockPolicy = DecisionPolicy.defaultPolicy();
        when(policyLoader.getCachedPolicy()).thenReturn(mockPolicy);

        String version = policyService.getPolicyVersion();

        assertEquals("default", version);
    }

    @Test
    void isAmountWithinLimits_withinLimit_returnsTrue() {
        DecisionPolicy policy = createPolicyWithLoanLimits(BigDecimal.valueOf(50000));
        when(policyLoader.getCachedPolicy()).thenReturn(policy);

        assertTrue(policyService.isAmountWithinLimits(BigDecimal.valueOf(25000)));
        assertTrue(policyService.isAmountWithinLimits(BigDecimal.valueOf(50000)));
    }

    @Test
    void isAmountWithinLimits_exceedsLimit_returnsFalse() {
        DecisionPolicy policy = createPolicyWithLoanLimits(BigDecimal.valueOf(50000));
        when(policyLoader.getCachedPolicy()).thenReturn(policy);

        assertFalse(policyService.isAmountWithinLimits(BigDecimal.valueOf(50001)));
        assertFalse(policyService.isAmountWithinLimits(BigDecimal.valueOf(100000)));
    }

    @Test
    void isIncomeAboveMinimum_aboveMinimum_returnsTrue() {
        DecisionPolicy policy = createPolicyWithMinIncome(BigDecimal.valueOf(1000));
        when(policyLoader.getCachedPolicy()).thenReturn(policy);

        assertTrue(policyService.isIncomeAboveMinimum(BigDecimal.valueOf(1000)));
        assertTrue(policyService.isIncomeAboveMinimum(BigDecimal.valueOf(5000)));
    }

    @Test
    void isIncomeAboveMinimum_belowMinimum_returnsFalse() {
        DecisionPolicy policy = createPolicyWithMinIncome(BigDecimal.valueOf(1000));
        when(policyLoader.getCachedPolicy()).thenReturn(policy);

        assertFalse(policyService.isIncomeAboveMinimum(BigDecimal.valueOf(999)));
        assertFalse(policyService.isIncomeAboveMinimum(BigDecimal.valueOf(500)));
    }

    @Test
    void isTermWithinLimits_withinLimits_returnsTrue() {
        DecisionPolicy policy = createPolicyWithTermLimits(6, 60);
        when(policyLoader.getCachedPolicy()).thenReturn(policy);

        assertTrue(policyService.isTermWithinLimits(6));
        assertTrue(policyService.isTermWithinLimits(24));
        assertTrue(policyService.isTermWithinLimits(60));
    }

    @Test
    void isTermWithinLimits_outsideLimits_returnsFalse() {
        DecisionPolicy policy = createPolicyWithTermLimits(6, 60);
        when(policyLoader.getCachedPolicy()).thenReturn(policy);

        assertFalse(policyService.isTermWithinLimits(5));
        assertFalse(policyService.isTermWithinLimits(61));
    }

    @Test
    void isCreditScoreAboveMinimum_aboveMinimum_returnsTrue() {
        DecisionPolicy policy = createPolicyWithMinCreditScore(600);
        when(policyLoader.getCachedPolicy()).thenReturn(policy);

        assertTrue(policyService.isCreditScoreAboveMinimum(600));
        assertTrue(policyService.isCreditScoreAboveMinimum(750));
    }

    @Test
    void isCreditScoreAboveMinimum_belowMinimum_returnsFalse() {
        DecisionPolicy policy = createPolicyWithMinCreditScore(600);
        when(policyLoader.getCachedPolicy()).thenReturn(policy);

        assertFalse(policyService.isCreditScoreAboveMinimum(599));
        assertFalse(policyService.isCreditScoreAboveMinimum(400));
    }

    private DecisionPolicy createPolicyWithLoanLimits(BigDecimal maxAmount) {
        return DecisionPolicy.builder()
                .riskThresholds(DecisionPolicy.RiskThresholds.builder()
                        .approve(BigDecimal.valueOf(0.05))
                        .review(BigDecimal.valueOf(0.15))
                        .build())
                .rules(DecisionPolicy.RulesPolicy.builder()
                        .hardFailDecline(true)
                        .maxSoftFailuresForApproval(2)
                        .build())
                .loanLimits(DecisionPolicy.LoanLimits.builder()
                        .maxAmount(maxAmount)
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
                        .version("test")
                        .effectiveDate("2024-01-01")
                        .description("Test policy")
                        .build())
                .build();
    }

    private DecisionPolicy createPolicyWithMinIncome(BigDecimal minIncome) {
        DecisionPolicy policy = createPolicyWithLoanLimits(BigDecimal.valueOf(50000));
        policy.getLoanLimits().setMinIncome(minIncome);
        return policy;
    }

    private DecisionPolicy createPolicyWithTermLimits(int minTerm, int maxTerm) {
        DecisionPolicy policy = createPolicyWithLoanLimits(BigDecimal.valueOf(50000));
        policy.getLoanLimits().setMinTermMonths(minTerm);
        policy.getLoanLimits().setMaxTermMonths(maxTerm);
        return policy;
    }

    private DecisionPolicy createPolicyWithMinCreditScore(int minScore) {
        DecisionPolicy policy = createPolicyWithLoanLimits(BigDecimal.valueOf(50000));
        policy.getCreditRequirements().setMinCreditScore(minScore);
        return policy;
    }
}
