package com.loan.decision.governance.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {

    private UUID id;
    private UUID decisionId;
    private String eventType;
    private String eventDescription;
    private String previousState;
    private String newState;
    private String ruleEvaluationSummary;
    private String riskAssessmentSummary;
    private String ruleResultsJson;
    private String riskAssessmentJson;
    private String performedBy;
    private LocalDateTime eventTimestamp;
}
