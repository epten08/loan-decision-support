package com.loan.decision.rules.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleEngineOutput {

    private List<RuleResultSummary> results;
    private boolean hardFailure;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RuleResultSummary {
        private String rule;
        private String result; // "PASS" or "FAIL"
    }

    public static RuleEngineOutput fromRuleResults(List<RuleResult> ruleResults) {
        List<RuleResultSummary> summaries = ruleResults.stream()
                .map(r -> RuleResultSummary.builder()
                        .rule(r.getRuleCode())
                        .result(r.isPassed() ? "PASS" : "FAIL")
                        .build())
                .toList();

        boolean hasHardFailure = ruleResults.stream()
                .anyMatch(RuleResult::isHardFail);

        return RuleEngineOutput.builder()
                .results(summaries)
                .hardFailure(hasHardFailure)
                .build();
    }
}
