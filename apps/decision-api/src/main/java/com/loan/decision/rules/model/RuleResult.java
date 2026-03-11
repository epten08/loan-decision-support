package com.loan.decision.rules.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleResult {

    private String ruleCode;
    private boolean passed;
    private RuleDefinition.RuleSeverity severity;
    private String reason;
    private String evaluatedCondition;
    private Object actualValue;

    public boolean isFailed() {
        return !passed;
    }

    public boolean isHardFail() {
        return !passed && severity == RuleDefinition.RuleSeverity.HARD;
    }
}
