package com.loan.decision.governance.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loan.decision.decisioning.model.Decision;
import com.loan.decision.decisioning.repository.DecisionRepository;
import com.loan.decision.governance.model.DecisionAuditLog;
import com.loan.decision.governance.repository.DecisionAuditLogRepository;
import com.loan.decision.loanintake.model.LoanApplication;
import com.loan.decision.riskadapter.model.RiskAssessment;
import com.loan.decision.rules.model.RuleResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service responsible for persisting decisions and creating audit trails.
 * Ensures all decision inputs are stored for reproducibility and compliance.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DecisionPersistenceService {

    private final DecisionRepository decisionRepository;
    private final DecisionAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * Saves the decision and creates a full audit trail with JSON snapshots.
     */
    @Transactional
    public Decision saveDecision(LoanApplication application,
                                  Decision.DecisionOutcome outcome,
                                  List<RuleResult> ruleResults,
                                  RiskAssessment riskAssessment,
                                  List<String> reasonCodes,
                                  String summary) {

        long hardFailures = ruleResults.stream().filter(RuleResult::isHardFail).count();
        long softFailures = ruleResults.stream()
                .filter(r -> r.isFailed() && !r.isHardFail()).count();

        Decision decision = Decision.builder()
                .loanApplication(application)
                .outcome(outcome)
                .reasonCodes(String.join(",", reasonCodes))
                .riskBand(riskAssessment.getRiskBand().name())
                .probabilityOfDefault(riskAssessment.getProbabilityOfDefault())
                .hardRuleFailures((int) hardFailures)
                .softRuleFailures((int) softFailures)
                .decisionSummary(summary)
                .modelVersion(riskAssessment.getModelVersion())
                .modelType(riskAssessment.getModelType())
                .build();

        Decision savedDecision = decisionRepository.save(decision);
        log.info("Saved decision {} for application {}: {}",
                savedDecision.getId(), application.getId(), outcome);

        // Create audit log with full JSON snapshots
        createAuditLog(savedDecision, ruleResults, riskAssessment);

        return savedDecision;
    }

    /**
     * Creates an audit log entry with full JSON snapshots for reproducibility.
     */
    @Transactional
    public void createAuditLog(Decision decision,
                                List<RuleResult> ruleResults,
                                RiskAssessment riskAssessment) {

        String rulesSummary = ruleResults.stream()
                .map(r -> String.format("%s:%s", r.getRuleCode(), r.isPassed() ? "PASS" : "FAIL"))
                .collect(Collectors.joining(";"));

        String riskSummary = String.format("PD:%.4f;Band:%s;Confidence:%.2f",
                riskAssessment.getProbabilityOfDefault(),
                riskAssessment.getRiskBand(),
                riskAssessment.getConfidence());

        DecisionAuditLog auditLog = DecisionAuditLog.builder()
                .decision(decision)
                .eventType(DecisionAuditLog.EventType.DECISION_MADE)
                .eventDescription("Automated decision evaluation completed")
                .previousState(null)
                .newState(decision.getOutcome().name())
                .ruleEvaluationSummary(rulesSummary)
                .riskAssessmentSummary(riskSummary)
                .ruleResultsJson(serializeRuleResults(ruleResults))
                .riskAssessmentJson(serializeRiskAssessment(riskAssessment))
                .build();

        auditLogRepository.save(auditLog);
        log.debug("Created audit log for decision {}", decision.getId());
    }

    /**
     * Creates an audit log for manual decision override.
     */
    @Transactional
    public void logDecisionOverride(Decision decision,
                                     Decision.DecisionOutcome previousOutcome,
                                     String performedBy,
                                     String reason) {

        DecisionAuditLog auditLog = DecisionAuditLog.builder()
                .decision(decision)
                .eventType(DecisionAuditLog.EventType.DECISION_OVERRIDDEN)
                .eventDescription(reason)
                .previousState(previousOutcome.name())
                .newState(decision.getOutcome().name())
                .performedBy(performedBy)
                .build();

        auditLogRepository.save(auditLog);
        log.info("Logged decision override for decision {} by {}", decision.getId(), performedBy);
    }

    private String serializeRuleResults(List<RuleResult> ruleResults) {
        try {
            List<Map<String, Object>> results = new ArrayList<>();
            for (RuleResult r : ruleResults) {
                Map<String, Object> map = new HashMap<>();
                map.put("ruleCode", r.getRuleCode());
                map.put("passed", r.isPassed());
                map.put("severity", r.getSeverity().name());
                map.put("reason", r.getReason());
                map.put("evaluatedCondition", r.getEvaluatedCondition());
                map.put("actualValue", r.getActualValue());
                results.add(map);
            }
            return objectMapper.writeValueAsString(results);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize rule results: {}", e.getMessage());
            return null;
        }
    }

    private String serializeRiskAssessment(RiskAssessment riskAssessment) {
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("probabilityOfDefault", riskAssessment.getProbabilityOfDefault());
            map.put("riskBand", riskAssessment.getRiskBand().name());
            map.put("confidence", riskAssessment.getConfidence());
            map.put("modelVersion", riskAssessment.getModelVersion());
            map.put("modelType", riskAssessment.getModelType());
            map.put("features", riskAssessment.getFeatures());
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize risk assessment: {}", e.getMessage());
            return null;
        }
    }
}
