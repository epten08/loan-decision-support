package com.loan.decision.rules.service;

import com.loan.decision.rules.model.RuleDefinition;
import com.loan.decision.rules.model.RuleResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class RuleEvaluator {

    private static final Pattern CONDITION_PATTERN = Pattern.compile(
            "(\\w+)\\s*(<=|>=|<>|!=|<|>|==|=)\\s*(.+)"
    );

    public RuleResult evaluate(RuleDefinition rule, Map<String, Object> applicationData) {
        log.debug("Evaluating rule: {} with condition: {}", rule.getCode(), rule.getCondition());

        try {
            // Condition describes the failure scenario - if condition is true, rule fails
            boolean conditionMet = evaluateCondition(rule.getCondition(), applicationData);
            boolean passed = !conditionMet;

            return RuleResult.builder()
                    .ruleCode(rule.getCode())
                    .passed(passed)
                    .severity(rule.getSeverity())
                    .reason(passed ? null : rule.getReason())
                    .evaluatedCondition(rule.getCondition())
                    .actualValue(extractActualValue(rule.getCondition(), applicationData))
                    .build();

        } catch (Exception e) {
            log.error("Error evaluating rule {}: {}", rule.getCode(), e.getMessage());
            return RuleResult.builder()
                    .ruleCode(rule.getCode())
                    .passed(false)
                    .severity(rule.getSeverity())
                    .reason("Rule evaluation error: " + e.getMessage())
                    .evaluatedCondition(rule.getCondition())
                    .build();
        }
    }

    private boolean evaluateCondition(String condition, Map<String, Object> data) {
        Matcher matcher = CONDITION_PATTERN.matcher(condition.trim());

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid condition format: " + condition);
        }

        String field = matcher.group(1);
        String operator = matcher.group(2);
        String expectedValue = matcher.group(3).trim();

        Object actualValue = data.get(field);
        if (actualValue == null) {
            log.warn("Field '{}' not found in application data - treating as rule violation", field);
            // Missing required data should trigger the failure condition
            return true;
        }

        return compareValues(actualValue, operator, expectedValue);
    }

    private boolean compareValues(Object actual, String operator, String expected) {
        if (actual instanceof Number) {
            BigDecimal actualNum = new BigDecimal(actual.toString());
            BigDecimal expectedNum = new BigDecimal(expected);

            return switch (operator) {
                case "<" -> actualNum.compareTo(expectedNum) < 0;
                case "<=" -> actualNum.compareTo(expectedNum) <= 0;
                case ">" -> actualNum.compareTo(expectedNum) > 0;
                case ">=" -> actualNum.compareTo(expectedNum) >= 0;
                case "==", "=" -> actualNum.compareTo(expectedNum) == 0;
                case "!=", "<>" -> actualNum.compareTo(expectedNum) != 0;
                default -> throw new IllegalArgumentException("Unknown operator: " + operator);
            };
        }

        String actualStr = actual.toString();
        return switch (operator) {
            case "==", "=" -> actualStr.equals(expected);
            case "!=", "<>" -> !actualStr.equals(expected);
            default -> throw new IllegalArgumentException("String comparison only supports == and !=");
        };
    }

    private Object extractActualValue(String condition, Map<String, Object> data) {
        Matcher matcher = CONDITION_PATTERN.matcher(condition.trim());
        if (matcher.matches()) {
            String field = matcher.group(1);
            return data.get(field);
        }
        return null;
    }
}
