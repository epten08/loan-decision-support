package com.loan.decision.rules.service;

import com.loan.decision.creditprofile.model.CreditProfile;
import com.loan.decision.loanintake.model.LoanApplication;
import com.loan.decision.rules.model.RuleDefinition;
import com.loan.decision.rules.model.RuleEvaluation;
import com.loan.decision.rules.model.RuleResult;
import com.loan.decision.rules.repository.RuleEvaluationRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RuleEngineService {

    private final RuleEvaluator ruleEvaluator;
    private final RuleEvaluationRepository ruleEvaluationRepository;

    @Value("${rules.definitions.path:classpath:rules/*.yaml}")
    private String rulesPath;

    private final List<RuleDefinition> ruleDefinitions = new ArrayList<>();

    @PostConstruct
    public void loadRules() {
        log.info("Loading rule definitions from: {}", rulesPath);

        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(rulesPath);

            Yaml yaml = new Yaml();

            for (Resource resource : resources) {
                log.info("Loading rules from: {}", resource.getFilename());
                try (InputStream is = resource.getInputStream()) {
                    List<Map<String, Object>> rules = yaml.load(is);
                    if (rules != null) {
                        for (Map<String, Object> ruleMap : rules) {
                            RuleDefinition rule = mapToRuleDefinition(ruleMap);
                            ruleDefinitions.add(rule);
                            log.debug("Loaded rule: {}", rule.getCode());
                        }
                    }
                }
            }

            log.info("Loaded {} rules", ruleDefinitions.size());

        } catch (Exception e) {
            log.warn("Could not load rule files: {}. Using default rules.", e.getMessage());
            loadDefaultRules();
        }
    }

    private void loadDefaultRules() {
        ruleDefinitions.add(RuleDefinition.builder()
                .code("MIN_INCOME")
                .condition("monthly_income < 500")
                .severity(RuleDefinition.RuleSeverity.HARD)
                .reason("INSUFFICIENT_INCOME")
                .description("Minimum monthly income requirement")
                .build());

        ruleDefinitions.add(RuleDefinition.builder()
                .code("MAX_DTI")
                .condition("debt_to_income_ratio > 50")
                .severity(RuleDefinition.RuleSeverity.HARD)
                .reason("HIGH_DEBT_TO_INCOME")
                .description("Maximum debt-to-income ratio")
                .build());

        ruleDefinitions.add(RuleDefinition.builder()
                .code("MIN_CREDIT_SCORE")
                .condition("credit_score < 300")
                .severity(RuleDefinition.RuleSeverity.HARD)
                .reason("LOW_CREDIT_SCORE")
                .description("Minimum credit score requirement")
                .build());

        ruleDefinitions.add(RuleDefinition.builder()
                .code("MAX_MISSED_PAYMENTS")
                .condition("missed_payments_last_12_months > 3")
                .severity(RuleDefinition.RuleSeverity.SOFT)
                .reason("PAYMENT_HISTORY_CONCERN")
                .description("Recent missed payments check")
                .build());

        ruleDefinitions.add(RuleDefinition.builder()
                .code("NO_RECENT_DEFAULTS")
                .condition("defaults_last_5_years > 0")
                .severity(RuleDefinition.RuleSeverity.HARD)
                .reason("PREVIOUS_DEFAULT")
                .description("No defaults in last 5 years")
                .build());

        log.info("Loaded {} default rules", ruleDefinitions.size());
    }

    private RuleDefinition mapToRuleDefinition(Map<String, Object> map) {
        return RuleDefinition.builder()
                .code((String) map.get("code"))
                .condition((String) map.get("condition"))
                .severity(RuleDefinition.RuleSeverity.valueOf(
                        ((String) map.get("severity")).toUpperCase()))
                .reason((String) map.get("reason"))
                .description((String) map.get("description"))
                .build();
    }

    @Transactional
    public List<RuleResult> evaluateApplication(LoanApplication application, CreditProfile creditProfile) {
        log.info("Evaluating rules for application: {}", application.getId());

        Map<String, Object> applicationData = buildApplicationData(application, creditProfile);

        List<RuleResult> results = new ArrayList<>();

        for (RuleDefinition rule : ruleDefinitions) {
            RuleResult result = ruleEvaluator.evaluate(rule, applicationData);
            results.add(result);

            // Persist evaluation
            RuleEvaluation evaluation = RuleEvaluation.builder()
                    .loanApplication(application)
                    .ruleCode(result.getRuleCode())
                    .passed(result.isPassed())
                    .severity(result.getSeverity())
                    .reason(result.getReason())
                    .evaluatedCondition(result.getEvaluatedCondition())
                    .actualValue(result.getActualValue() != null ? result.getActualValue().toString() : null)
                    .build();

            ruleEvaluationRepository.save(evaluation);

            log.debug("Rule {} - Passed: {}", result.getRuleCode(), result.isPassed());
        }

        long hardFails = results.stream().filter(RuleResult::isHardFail).count();
        long softFails = results.stream().filter(r -> r.isFailed() && !r.isHardFail()).count();

        log.info("Rule evaluation complete. Hard fails: {}, Soft fails: {}", hardFails, softFails);

        return results;
    }

    private Map<String, Object> buildApplicationData(LoanApplication application, CreditProfile creditProfile) {
        Map<String, Object> data = new HashMap<>();

        // Application data
        data.put("monthly_income", application.getMonthlyIncome());
        data.put("monthly_expenses", application.getMonthlyExpenses());
        data.put("requested_amount", application.getRequestedAmount());
        data.put("term_months", application.getTermMonths());

        // Credit profile data
        if (creditProfile != null) {
            data.put("credit_score", creditProfile.getCreditScore());
            data.put("debt_to_income_ratio", creditProfile.getDebtToIncomeRatio());
            data.put("existing_loan_count", creditProfile.getExistingLoanCount());
            data.put("total_existing_debt", creditProfile.getTotalExistingDebt());
            data.put("monthly_debt_payments", creditProfile.getMonthlyDebtPayments());
            data.put("credit_history_months", creditProfile.getCreditHistoryMonths());
            data.put("missed_payments_last_12_months", creditProfile.getMissedPaymentsLast12Months());
            data.put("defaults_last_5_years", creditProfile.getDefaultsLast5Years());
        }

        return data;
    }

    public List<RuleDefinition> getAllRules() {
        return Collections.unmodifiableList(ruleDefinitions);
    }
}
