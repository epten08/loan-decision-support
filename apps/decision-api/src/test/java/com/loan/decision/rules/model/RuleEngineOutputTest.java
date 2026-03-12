package com.loan.decision.rules.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RuleEngineOutputTest {

    @Test
    void fromRuleResults_noFailures_returnsNoHardFailure() {
        List<RuleResult> results = Arrays.asList(
                RuleResult.builder()
                        .ruleCode("MIN_INCOME")
                        .passed(true)
                        .severity(RuleDefinition.RuleSeverity.HARD)
                        .build(),
                RuleResult.builder()
                        .ruleCode("MIN_CREDIT_SCORE")
                        .passed(true)
                        .severity(RuleDefinition.RuleSeverity.HARD)
                        .build()
        );

        RuleEngineOutput output = RuleEngineOutput.fromRuleResults(results);

        assertFalse(output.isHardFailure());
        assertEquals(2, output.getResults().size());
        assertEquals("PASS", output.getResults().get(0).getResult());
        assertEquals("PASS", output.getResults().get(1).getResult());
    }

    @Test
    void fromRuleResults_withHardFailure_returnsHardFailure() {
        List<RuleResult> results = Arrays.asList(
                RuleResult.builder()
                        .ruleCode("MIN_INCOME")
                        .passed(true)
                        .severity(RuleDefinition.RuleSeverity.HARD)
                        .build(),
                RuleResult.builder()
                        .ruleCode("MIN_CREDIT_SCORE")
                        .passed(false)
                        .severity(RuleDefinition.RuleSeverity.HARD)
                        .reason("LOW_CREDIT_SCORE")
                        .build()
        );

        RuleEngineOutput output = RuleEngineOutput.fromRuleResults(results);

        assertTrue(output.isHardFailure());
        assertEquals(2, output.getResults().size());
        assertEquals("PASS", output.getResults().get(0).getResult());
        assertEquals("FAIL", output.getResults().get(1).getResult());
    }

    @Test
    void fromRuleResults_withSoftFailureOnly_returnsNoHardFailure() {
        List<RuleResult> results = Arrays.asList(
                RuleResult.builder()
                        .ruleCode("MIN_INCOME")
                        .passed(true)
                        .severity(RuleDefinition.RuleSeverity.HARD)
                        .build(),
                RuleResult.builder()
                        .ruleCode("MAX_MISSED_PAYMENTS")
                        .passed(false)
                        .severity(RuleDefinition.RuleSeverity.SOFT)
                        .reason("PAYMENT_HISTORY_CONCERN")
                        .build()
        );

        RuleEngineOutput output = RuleEngineOutput.fromRuleResults(results);

        assertFalse(output.isHardFailure());
        assertEquals(2, output.getResults().size());
        assertEquals("FAIL", output.getResults().get(1).getResult());
    }

    @Test
    void fromRuleResults_multipleHardFailures_returnsHardFailure() {
        List<RuleResult> results = Arrays.asList(
                RuleResult.builder()
                        .ruleCode("MIN_INCOME")
                        .passed(false)
                        .severity(RuleDefinition.RuleSeverity.HARD)
                        .reason("INSUFFICIENT_INCOME")
                        .build(),
                RuleResult.builder()
                        .ruleCode("MIN_CREDIT_SCORE")
                        .passed(false)
                        .severity(RuleDefinition.RuleSeverity.HARD)
                        .reason("LOW_CREDIT_SCORE")
                        .build(),
                RuleResult.builder()
                        .ruleCode("EMPLOYMENT_REQUIRED")
                        .passed(false)
                        .severity(RuleDefinition.RuleSeverity.HARD)
                        .reason("EMPLOYMENT_REQUIRED")
                        .build()
        );

        RuleEngineOutput output = RuleEngineOutput.fromRuleResults(results);

        assertTrue(output.isHardFailure());
        assertEquals(3, output.getResults().size());
        assertTrue(output.getResults().stream().allMatch(r -> r.getResult().equals("FAIL")));
    }

    @Test
    void fromRuleResults_preservesRuleOrder() {
        List<RuleResult> results = Arrays.asList(
                RuleResult.builder().ruleCode("RULE_A").passed(true).severity(RuleDefinition.RuleSeverity.HARD).build(),
                RuleResult.builder().ruleCode("RULE_B").passed(false).severity(RuleDefinition.RuleSeverity.SOFT).build(),
                RuleResult.builder().ruleCode("RULE_C").passed(true).severity(RuleDefinition.RuleSeverity.HARD).build()
        );

        RuleEngineOutput output = RuleEngineOutput.fromRuleResults(results);

        assertEquals("RULE_A", output.getResults().get(0).getRule());
        assertEquals("RULE_B", output.getResults().get(1).getRule());
        assertEquals("RULE_C", output.getResults().get(2).getRule());
    }
}
