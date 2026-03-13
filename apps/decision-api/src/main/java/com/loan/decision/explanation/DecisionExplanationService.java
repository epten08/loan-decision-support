package com.loan.decision.explanation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loan.decision.decisioning.model.Decision;
import com.loan.decision.decisioning.repository.DecisionRepository;
import com.loan.decision.governance.model.DecisionAuditLog;
import com.loan.decision.governance.repository.DecisionAuditLogRepository;
import com.loan.decision.policy.DecisionPolicy;
import com.loan.decision.policy.PolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * Service for generating decision explanations.
 * Combines data from decisions, audit logs, and policy configuration
 * to produce human-readable explanations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DecisionExplanationService {

    private final DecisionRepository decisionRepository;
    private final DecisionAuditLogRepository auditLogRepository;
    private final PolicyService policyService;
    private final ObjectMapper objectMapper;

    /**
     * Generates a complete explanation for a loan decision.
     *
     * @param applicationId the loan application ID
     * @return the decision explanation
     * @throws IllegalArgumentException if no decision exists for the application
     */
    @Transactional(readOnly = true)
    public DecisionExplanation explain(UUID applicationId) {
        log.info("Generating explanation for application: {}", applicationId);

        // Load decision
        Decision decision = decisionRepository.findByLoanApplicationId(applicationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No decision found for application: " + applicationId));

        // Load audit log for detailed information
        List<DecisionAuditLog> auditLogs = auditLogRepository
                .findByDecisionLoanApplicationIdOrderByEventTimestampDesc(applicationId);

        DecisionAuditLog decisionMadeLog = auditLogs.stream()
                .filter(log -> log.getEventType() == DecisionAuditLog.EventType.DECISION_MADE)
                .findFirst()
                .orElse(null);

        // Get current policy (or reconstruct from audit if available)
        DecisionPolicy policy = policyService.getCurrentPolicy();

        // Build explanation
        return DecisionExplanation.builder()
                .applicationId(applicationId)
                .decision(decision.getOutcome().name())
                .decisionReason(buildDecisionReason(decision))
                .riskAssessment(buildRiskAssessmentExplanation(decision, decisionMadeLog))
                .rules(buildRuleResultExplanations(decisionMadeLog))
                .policy(buildPolicySnapshot(policy))
                .timestamp(decision.getDecidedAt())
                .decidedBy(decision.getDecidedBy())
                .build();
    }

    /**
     * Generates a summary explanation (without full rule details).
     */
    @Transactional(readOnly = true)
    public DecisionExplanation explainSummary(UUID applicationId) {
        Decision decision = decisionRepository.findByLoanApplicationId(applicationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No decision found for application: " + applicationId));

        DecisionPolicy policy = policyService.getCurrentPolicy();

        return DecisionExplanation.builder()
                .applicationId(applicationId)
                .decision(decision.getOutcome().name())
                .decisionReason(buildDecisionReason(decision))
                .riskAssessment(DecisionExplanation.RiskAssessmentExplanation.builder()
                        .probabilityOfDefault(decision.getProbabilityOfDefault())
                        .riskBand(decision.getRiskBand())
                        .interpretation(interpretRiskBand(decision.getRiskBand()))
                        .build())
                .policy(buildPolicySnapshot(policy))
                .timestamp(decision.getDecidedAt())
                .build();
    }

    private String buildDecisionReason(Decision decision) {
        String summary = decision.getDecisionSummary();
        if (summary != null && !summary.isEmpty()) {
            return summary;
        }

        // Fallback to constructing reason from outcome and data
        return switch (decision.getOutcome()) {
            case APPROVED -> "Application meets all approval criteria";
            case DECLINED -> buildDeclineReason(decision);
            case MANUAL_REVIEW -> "Application requires manual review by underwriter";
            case CONDITIONAL -> "Application conditionally approved pending additional information";
            case PENDING_REVIEW -> "Application pending review";
        };
    }

    private String buildDeclineReason(Decision decision) {
        List<String> reasons = new ArrayList<>();

        if (decision.getHardRuleFailures() != null && decision.getHardRuleFailures() > 0) {
            reasons.add(String.format("%d hard rule failure(s)", decision.getHardRuleFailures()));
        }

        if (decision.getProbabilityOfDefault() != null) {
            DecisionPolicy policy = policyService.getCurrentPolicy();
            if (decision.getProbabilityOfDefault().compareTo(policy.getReviewPdThreshold()) >= 0) {
                reasons.add(String.format("Risk score (%.1f%%) exceeds threshold",
                        decision.getProbabilityOfDefault().multiply(BigDecimal.valueOf(100))));
            }
        }

        if (reasons.isEmpty()) {
            return "Application did not meet approval criteria";
        }

        return String.join("; ", reasons);
    }

    private DecisionExplanation.RiskAssessmentExplanation buildRiskAssessmentExplanation(
            Decision decision, DecisionAuditLog auditLog) {

        BigDecimal pd = decision.getProbabilityOfDefault();
        String riskBand = decision.getRiskBand();
        BigDecimal confidence = null;

        // Try to extract confidence from audit log
        if (auditLog != null && auditLog.getRiskAssessmentJson() != null) {
            try {
                Map<String, Object> riskData = objectMapper.readValue(
                        auditLog.getRiskAssessmentJson(),
                        new TypeReference<Map<String, Object>>() {});
                if (riskData.containsKey("confidence")) {
                    confidence = new BigDecimal(riskData.get("confidence").toString());
                }
            } catch (Exception e) {
                log.warn("Failed to parse risk assessment JSON: {}", e.getMessage());
            }
        }

        return DecisionExplanation.RiskAssessmentExplanation.builder()
                .probabilityOfDefault(pd)
                .riskBand(riskBand)
                .confidence(confidence)
                .interpretation(interpretRiskBand(riskBand))
                .build();
    }

    private String interpretRiskBand(String riskBand) {
        if (riskBand == null) {
            return "Risk band not available";
        }

        return switch (riskBand) {
            case "A" -> "Excellent credit risk - very low probability of default";
            case "B" -> "Good credit risk - low probability of default";
            case "C" -> "Fair credit risk - moderate probability of default";
            case "D" -> "Poor credit risk - elevated probability of default";
            case "E" -> "High credit risk - significant probability of default";
            default -> "Unknown risk band";
        };
    }

    private List<DecisionExplanation.RuleResultExplanation> buildRuleResultExplanations(
            DecisionAuditLog auditLog) {

        if (auditLog == null || auditLog.getRuleResultsJson() == null) {
            return Collections.emptyList();
        }

        try {
            List<Map<String, Object>> ruleResults = objectMapper.readValue(
                    auditLog.getRuleResultsJson(),
                    new TypeReference<List<Map<String, Object>>>() {});

            return ruleResults.stream()
                    .map(this::mapToRuleResultExplanation)
                    .toList();
        } catch (Exception e) {
            log.warn("Failed to parse rule results JSON: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private DecisionExplanation.RuleResultExplanation mapToRuleResultExplanation(
            Map<String, Object> ruleData) {

        String ruleCode = getStringValue(ruleData, "ruleCode");
        boolean passed = getBooleanValue(ruleData, "passed");
        String severity = getStringValue(ruleData, "severity");
        String reason = getStringValue(ruleData, "reason");
        String description = getStringValue(ruleData, "description");

        return DecisionExplanation.RuleResultExplanation.builder()
                .rule(ruleCode)
                .result(passed ? "PASS" : "FAIL")
                .severity(severity)
                .description(description)
                .reason(passed ? null : reason)
                .build();
    }

    private DecisionExplanation.PolicySnapshot buildPolicySnapshot(DecisionPolicy policy) {
        return DecisionExplanation.PolicySnapshot.builder()
                .approveThreshold(policy.getApprovePdThreshold())
                .reviewThreshold(policy.getReviewPdThreshold())
                .maxSoftFailuresForApproval(policy.getMaxSoftFailuresForApproval())
                .hardFailDecline(policy.isHardFailDecline())
                .policyVersion(policy.getMetadata().getVersion())
                .build();
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private boolean getBooleanValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return "true".equalsIgnoreCase(String.valueOf(value));
    }
}
