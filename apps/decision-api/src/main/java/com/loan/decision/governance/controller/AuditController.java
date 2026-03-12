package com.loan.decision.governance.controller;

import com.loan.decision.governance.controller.dto.AuditLogResponse;
import com.loan.decision.governance.model.DecisionAuditLog;
import com.loan.decision.governance.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@Slf4j
public class AuditController {

    private final AuditService auditService;

    /**
     * Retrieves the full audit trail for a loan application.
     * Includes rule results and risk assessment JSON snapshots for reproducibility.
     */
    @GetMapping("/applications/{applicationId}")
    public ResponseEntity<List<AuditLogResponse>> getAuditTrail(@PathVariable UUID applicationId) {
        log.info("Retrieving audit trail for application: {}", applicationId);
        List<DecisionAuditLog> auditLogs = auditService.getAuditTrail(applicationId);
        List<AuditLogResponse> response = auditLogs.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves audit logs for a specific decision.
     */
    @GetMapping("/decisions/{decisionId}")
    public ResponseEntity<List<AuditLogResponse>> getDecisionAuditLogs(@PathVariable UUID decisionId) {
        log.info("Retrieving audit logs for decision: {}", decisionId);
        List<DecisionAuditLog> auditLogs = auditService.getAuditLogsByDecision(decisionId);
        List<AuditLogResponse> response = auditLogs.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    private AuditLogResponse mapToResponse(DecisionAuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .decisionId(log.getDecision().getId())
                .eventType(log.getEventType().name())
                .eventDescription(log.getEventDescription())
                .previousState(log.getPreviousState())
                .newState(log.getNewState())
                .ruleEvaluationSummary(log.getRuleEvaluationSummary())
                .riskAssessmentSummary(log.getRiskAssessmentSummary())
                .ruleResultsJson(log.getRuleResultsJson())
                .riskAssessmentJson(log.getRiskAssessmentJson())
                .performedBy(log.getPerformedBy())
                .eventTimestamp(log.getEventTimestamp())
                .build();
    }
}
