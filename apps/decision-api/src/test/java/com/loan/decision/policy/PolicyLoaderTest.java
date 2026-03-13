package com.loan.decision.policy;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class PolicyLoaderTest {

    private final ResourceLoader resourceLoader = new DefaultResourceLoader();

    @Test
    void loadPolicy_validYaml_loadsSuccessfully() {
        PolicyLoader loader = new PolicyLoader(
                resourceLoader,
                "classpath:policy/decision-policy.yaml"
        );

        DecisionPolicy policy = loader.loadPolicy();

        assertNotNull(policy);
        assertNotNull(policy.getRiskThresholds());
        assertNotNull(policy.getRules());
        assertNotNull(policy.getLoanLimits());
        assertNotNull(policy.getCreditRequirements());
        assertNotNull(policy.getMetadata());
    }

    @Test
    void loadPolicy_validYaml_hasCorrectRiskThresholds() {
        PolicyLoader loader = new PolicyLoader(
                resourceLoader,
                "classpath:policy/decision-policy.yaml"
        );

        DecisionPolicy policy = loader.loadPolicy();

        assertEquals(0, BigDecimal.valueOf(0.05).compareTo(policy.getApprovePdThreshold()));
        assertEquals(0, BigDecimal.valueOf(0.15).compareTo(policy.getReviewPdThreshold()));
    }

    @Test
    void loadPolicy_validYaml_hasCorrectRulesPolicy() {
        PolicyLoader loader = new PolicyLoader(
                resourceLoader,
                "classpath:policy/decision-policy.yaml"
        );

        DecisionPolicy policy = loader.loadPolicy();

        assertTrue(policy.isHardFailDecline());
        assertEquals(2, policy.getMaxSoftFailuresForApproval());
    }

    @Test
    void loadPolicy_validYaml_hasCorrectLoanLimits() {
        PolicyLoader loader = new PolicyLoader(
                resourceLoader,
                "classpath:policy/decision-policy.yaml"
        );

        DecisionPolicy policy = loader.loadPolicy();

        assertEquals(0, BigDecimal.valueOf(50000).compareTo(policy.getLoanLimits().getMaxAmount()));
        assertEquals(0, BigDecimal.valueOf(1000).compareTo(policy.getLoanLimits().getMinIncome()));
        assertEquals(60, policy.getLoanLimits().getMaxTermMonths());
        assertEquals(6, policy.getLoanLimits().getMinTermMonths());
    }

    @Test
    void loadPolicy_validYaml_hasCorrectCreditRequirements() {
        PolicyLoader loader = new PolicyLoader(
                resourceLoader,
                "classpath:policy/decision-policy.yaml"
        );

        DecisionPolicy policy = loader.loadPolicy();

        assertEquals(600, policy.getCreditRequirements().getMinCreditScore());
        assertEquals(0, BigDecimal.valueOf(0.40).compareTo(policy.getCreditRequirements().getMaxDtiRatio()));
        assertEquals(6, policy.getCreditRequirements().getMinCreditHistoryMonths());
    }

    @Test
    void loadPolicy_validYaml_hasMetadata() {
        PolicyLoader loader = new PolicyLoader(
                resourceLoader,
                "classpath:policy/decision-policy.yaml"
        );

        DecisionPolicy policy = loader.loadPolicy();

        assertEquals("1.0.0", policy.getMetadata().getVersion());
        assertEquals("2024-01-01", policy.getMetadata().getEffectiveDate());
        assertNotNull(policy.getMetadata().getDescription());
    }

    @Test
    void loadPolicy_fileNotFound_usesDefaultPolicy() {
        PolicyLoader loader = new PolicyLoader(
                resourceLoader,
                "classpath:policy/nonexistent.yaml"
        );

        DecisionPolicy policy = loader.loadPolicy();

        assertNotNull(policy);
        assertEquals("default", policy.getMetadata().getVersion());
    }

    @Test
    void getCachedPolicy_afterLoad_returnsCachedVersion() {
        PolicyLoader loader = new PolicyLoader(
                resourceLoader,
                "classpath:policy/decision-policy.yaml"
        );

        DecisionPolicy first = loader.loadPolicy();
        DecisionPolicy second = loader.getCachedPolicy();

        assertSame(first, second);
    }

    @Test
    void reloadPolicy_refreshesCache() {
        PolicyLoader loader = new PolicyLoader(
                resourceLoader,
                "classpath:policy/decision-policy.yaml"
        );

        DecisionPolicy first = loader.loadPolicy();
        DecisionPolicy reloaded = loader.reloadPolicy();

        // Should be a new instance (same values, different object)
        assertNotNull(reloaded);
        assertEquals(first.getMetadata().getVersion(), reloaded.getMetadata().getVersion());
    }
}
