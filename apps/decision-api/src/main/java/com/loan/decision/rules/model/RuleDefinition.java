package com.loan.decision.rules.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleDefinition {

    private String code;
    private String condition;
    private RuleSeverity severity;
    private String reason;
    private String description;

    public enum RuleSeverity {
        HARD,  // Automatic decline if failed
        SOFT   // Warning, may still approve
    }
}
