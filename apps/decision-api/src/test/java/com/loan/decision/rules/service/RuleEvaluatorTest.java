package com.loan.decision.rules.service;

import com.loan.decision.rules.model.RuleDefinition;
import com.loan.decision.rules.model.RuleResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RuleEvaluatorTest {

    private RuleEvaluator ruleEvaluator;

    @BeforeEach
    void setUp() {
        ruleEvaluator = new RuleEvaluator();
    }

    @Test
    void evaluate_minIncome_passesWhenIncomeAboveThreshold() {
        RuleDefinition rule = RuleDefinition.builder()
                .code("MIN_INCOME")
                .condition("monthly_income < 500")
                .severity(RuleDefinition.RuleSeverity.HARD)
                .reason("INSUFFICIENT_INCOME")
                .build();

        Map<String, Object> data = new HashMap<>();
        data.put("monthly_income", BigDecimal.valueOf(5000));

        RuleResult result = ruleEvaluator.evaluate(rule, data);

        assertTrue(result.isPassed());
        assertNull(result.getReason());
    }

    @Test
    void evaluate_minIncome_failsWhenIncomeBelowThreshold() {
        RuleDefinition rule = RuleDefinition.builder()
                .code("MIN_INCOME")
                .condition("monthly_income < 500")
                .severity(RuleDefinition.RuleSeverity.HARD)
                .reason("INSUFFICIENT_INCOME")
                .build();

        Map<String, Object> data = new HashMap<>();
        data.put("monthly_income", BigDecimal.valueOf(400));

        RuleResult result = ruleEvaluator.evaluate(rule, data);

        assertFalse(result.isPassed());
        assertTrue(result.isHardFail());
        assertEquals("INSUFFICIENT_INCOME", result.getReason());
    }

    @Test
    void evaluate_maxDti_passesWhenDtiBelowThreshold() {
        RuleDefinition rule = RuleDefinition.builder()
                .code("MAX_DTI")
                .condition("debt_to_income_ratio > 0.4")
                .severity(RuleDefinition.RuleSeverity.HARD)
                .reason("HIGH_DEBT_TO_INCOME")
                .build();

        Map<String, Object> data = new HashMap<>();
        data.put("debt_to_income_ratio", BigDecimal.valueOf(0.3));

        RuleResult result = ruleEvaluator.evaluate(rule, data);

        assertTrue(result.isPassed());
    }

    @Test
    void evaluate_maxDti_failsWhenDtiAboveThreshold() {
        RuleDefinition rule = RuleDefinition.builder()
                .code("MAX_DTI")
                .condition("debt_to_income_ratio > 0.4")
                .severity(RuleDefinition.RuleSeverity.HARD)
                .reason("HIGH_DEBT_TO_INCOME")
                .build();

        Map<String, Object> data = new HashMap<>();
        data.put("debt_to_income_ratio", BigDecimal.valueOf(0.5));

        RuleResult result = ruleEvaluator.evaluate(rule, data);

        assertFalse(result.isPassed());
        assertTrue(result.isHardFail());
    }

    @Test
    void evaluate_minCreditScore_passesWhenScoreAboveThreshold() {
        RuleDefinition rule = RuleDefinition.builder()
                .code("MIN_CREDIT_SCORE")
                .condition("credit_score < 600")
                .severity(RuleDefinition.RuleSeverity.HARD)
                .reason("LOW_CREDIT_SCORE")
                .build();

        Map<String, Object> data = new HashMap<>();
        data.put("credit_score", 700);

        RuleResult result = ruleEvaluator.evaluate(rule, data);

        assertTrue(result.isPassed());
    }

    @Test
    void evaluate_minCreditScore_failsWhenScoreBelowThreshold() {
        RuleDefinition rule = RuleDefinition.builder()
                .code("MIN_CREDIT_SCORE")
                .condition("credit_score < 600")
                .severity(RuleDefinition.RuleSeverity.HARD)
                .reason("LOW_CREDIT_SCORE")
                .build();

        Map<String, Object> data = new HashMap<>();
        data.put("credit_score", 550);

        RuleResult result = ruleEvaluator.evaluate(rule, data);

        assertFalse(result.isPassed());
        assertTrue(result.isHardFail());
    }

    @Test
    void evaluate_maxLoanAmount_passesWhenAmountBelowLimit() {
        RuleDefinition rule = RuleDefinition.builder()
                .code("MAX_LOAN_AMOUNT")
                .condition("requested_amount > 20000")
                .severity(RuleDefinition.RuleSeverity.HARD)
                .reason("EXCEEDS_POLICY_LIMIT")
                .build();

        Map<String, Object> data = new HashMap<>();
        data.put("requested_amount", BigDecimal.valueOf(15000));

        RuleResult result = ruleEvaluator.evaluate(rule, data);

        assertTrue(result.isPassed());
    }

    @Test
    void evaluate_maxLoanAmount_failsWhenAmountAboveLimit() {
        RuleDefinition rule = RuleDefinition.builder()
                .code("MAX_LOAN_AMOUNT")
                .condition("requested_amount > 20000")
                .severity(RuleDefinition.RuleSeverity.HARD)
                .reason("EXCEEDS_POLICY_LIMIT")
                .build();

        Map<String, Object> data = new HashMap<>();
        data.put("requested_amount", BigDecimal.valueOf(25000));

        RuleResult result = ruleEvaluator.evaluate(rule, data);

        assertFalse(result.isPassed());
        assertTrue(result.isHardFail());
    }

    @Test
    void evaluate_employmentRequired_passesWhenEmployed() {
        RuleDefinition rule = RuleDefinition.builder()
                .code("EMPLOYMENT_REQUIRED")
                .condition("employment_status != EMPLOYED")
                .severity(RuleDefinition.RuleSeverity.HARD)
                .reason("EMPLOYMENT_REQUIRED")
                .build();

        Map<String, Object> data = new HashMap<>();
        data.put("employment_status", "EMPLOYED");

        RuleResult result = ruleEvaluator.evaluate(rule, data);

        assertTrue(result.isPassed());
    }

    @Test
    void evaluate_employmentRequired_failsWhenUnemployed() {
        RuleDefinition rule = RuleDefinition.builder()
                .code("EMPLOYMENT_REQUIRED")
                .condition("employment_status != EMPLOYED")
                .severity(RuleDefinition.RuleSeverity.HARD)
                .reason("EMPLOYMENT_REQUIRED")
                .build();

        Map<String, Object> data = new HashMap<>();
        data.put("employment_status", "UNEMPLOYED");

        RuleResult result = ruleEvaluator.evaluate(rule, data);

        assertFalse(result.isPassed());
        assertTrue(result.isHardFail());
    }

    @Test
    void evaluate_employmentRequired_failsWhenSelfEmployed() {
        RuleDefinition rule = RuleDefinition.builder()
                .code("EMPLOYMENT_REQUIRED")
                .condition("employment_status != EMPLOYED")
                .severity(RuleDefinition.RuleSeverity.HARD)
                .reason("EMPLOYMENT_REQUIRED")
                .build();

        Map<String, Object> data = new HashMap<>();
        data.put("employment_status", "SELF_EMPLOYED");

        RuleResult result = ruleEvaluator.evaluate(rule, data);

        assertFalse(result.isPassed());
    }

    @Test
    void evaluate_softRule_failsButNotHardFail() {
        RuleDefinition rule = RuleDefinition.builder()
                .code("MAX_MISSED_PAYMENTS")
                .condition("missed_payments_last_12_months > 3")
                .severity(RuleDefinition.RuleSeverity.SOFT)
                .reason("PAYMENT_HISTORY_CONCERN")
                .build();

        Map<String, Object> data = new HashMap<>();
        data.put("missed_payments_last_12_months", 5);

        RuleResult result = ruleEvaluator.evaluate(rule, data);

        assertFalse(result.isPassed());
        assertTrue(result.isFailed());
        assertFalse(result.isHardFail()); // Soft rule - not a hard fail
    }

    @Test
    void evaluate_missingField_failsGracefully() {
        RuleDefinition rule = RuleDefinition.builder()
                .code("MIN_INCOME")
                .condition("monthly_income < 500")
                .severity(RuleDefinition.RuleSeverity.HARD)
                .reason("INSUFFICIENT_INCOME")
                .build();

        Map<String, Object> data = new HashMap<>();
        // monthly_income not present

        RuleResult result = ruleEvaluator.evaluate(rule, data);

        assertFalse(result.isPassed());
    }

    @Test
    void evaluate_equalityOperator_works() {
        RuleDefinition rule = RuleDefinition.builder()
                .code("EXACT_INCOME")
                .condition("monthly_income == 500")
                .severity(RuleDefinition.RuleSeverity.SOFT)
                .reason("EXACT_THRESHOLD")
                .build();

        Map<String, Object> data = new HashMap<>();
        data.put("monthly_income", BigDecimal.valueOf(500));

        RuleResult result = ruleEvaluator.evaluate(rule, data);

        // Condition true means failure (condition describes failure scenario)
        assertFalse(result.isPassed());
    }

    @Test
    void evaluate_lessThanOrEqual_works() {
        RuleDefinition rule = RuleDefinition.builder()
                .code("TEST_RULE")
                .condition("credit_score <= 600")
                .severity(RuleDefinition.RuleSeverity.HARD)
                .reason("TEST")
                .build();

        Map<String, Object> data = new HashMap<>();
        data.put("credit_score", 600);

        RuleResult result = ruleEvaluator.evaluate(rule, data);

        // score = 600, condition credit_score <= 600 is true, so rule FAILS
        assertFalse(result.isPassed());
    }

    @Test
    void evaluate_greaterThanOrEqual_works() {
        RuleDefinition rule = RuleDefinition.builder()
                .code("TEST_RULE")
                .condition("credit_score >= 600")
                .severity(RuleDefinition.RuleSeverity.SOFT)
                .reason("TEST")
                .build();

        Map<String, Object> data = new HashMap<>();
        data.put("credit_score", 600);

        RuleResult result = ruleEvaluator.evaluate(rule, data);

        // score = 600, condition >= 600 is true, so rule FAILS
        assertFalse(result.isPassed());
    }
}
