package com.loan.decision.explanation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loan.decision.decisioning.model.Decision;
import com.loan.decision.decisioning.repository.DecisionRepository;
import com.loan.decision.governance.model.DecisionAuditLog;
import com.loan.decision.governance.repository.DecisionAuditLogRepository;
import com.loan.decision.loanintake.model.LoanApplication;
import com.loan.decision.policy.DecisionPolicy;
import com.loan.decision.policy.PolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DecisionExplanationServiceTest {

    @Mock
    private DecisionRepository decisionRepository;

    @Mock
    private DecisionAuditLogRepository auditLogRepository;

    @Mock
    private PolicyService policyService;

    private ObjectMapper objectMapper;
    private DecisionExplanationService explanationService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        explanationService = new DecisionExplanationService(
                decisionRepository,
                auditLogRepository,
                policyService,
                objectMapper
        );
    }

    @Test
    void explain_approvedDecision_returnsCompleteExplanation() {
        // Arrange
        UUID applicationId = UUID.randomUUID();
        Decision decision = createTestDecision(applicationId, Decision.DecisionOutcome.APPROVED);
        DecisionAuditLog auditLog = createTestAuditLog(decision);
        DecisionPolicy policy = DecisionPolicy.defaultPolicy();

        when(decisionRepository.findByLoanApplicationId(applicationId))
                .thenReturn(Optional.of(decision));
        when(auditLogRepository.findByDecisionLoanApplicationIdOrderByEventTimestampDesc(applicationId))
                .thenReturn(List.of(auditLog));
        when(policyService.getCurrentPolicy()).thenReturn(policy);

        // Act
        DecisionExplanation explanation = explanationService.explain(applicationId);

        // Assert
        assertNotNull(explanation);
        assertEquals(applicationId, explanation.getApplicationId());
        assertEquals("APPROVED", explanation.getDecision());
        assertNotNull(explanation.getDecisionReason());
        assertNotNull(explanation.getRiskAssessment());
        assertNotNull(explanation.getPolicy());
        assertEquals(BigDecimal.valueOf(0.05), explanation.getPolicy().getApproveThreshold());
    }

    @Test
    void explain_declinedDecision_includesDeclineReason() {
        // Arrange
        UUID applicationId = UUID.randomUUID();
        Decision decision = createTestDecision(applicationId, Decision.DecisionOutcome.DECLINED);
        decision.setHardRuleFailures(2);
        decision.setDecisionSummary("Declined due to 2 hard rule failure(s)");
        DecisionPolicy policy = DecisionPolicy.defaultPolicy();

        when(decisionRepository.findByLoanApplicationId(applicationId))
                .thenReturn(Optional.of(decision));
        when(auditLogRepository.findByDecisionLoanApplicationIdOrderByEventTimestampDesc(applicationId))
                .thenReturn(Collections.emptyList());
        when(policyService.getCurrentPolicy()).thenReturn(policy);

        // Act
        DecisionExplanation explanation = explanationService.explain(applicationId);

        // Assert
        assertEquals("DECLINED", explanation.getDecision());
        assertTrue(explanation.getDecisionReason().contains("hard rule failure"));
    }

    @Test
    void explain_withRuleResults_parsesRulesCorrectly() throws Exception {
        // Arrange
        UUID applicationId = UUID.randomUUID();
        Decision decision = createTestDecision(applicationId, Decision.DecisionOutcome.APPROVED);

        String ruleResultsJson = """
                [
                    {"ruleCode": "MIN_INCOME", "passed": true, "severity": "HARD"},
                    {"ruleCode": "MAX_DTI", "passed": false, "severity": "SOFT", "reason": "HIGH_DTI"}
                ]
                """;

        DecisionAuditLog auditLog = createTestAuditLog(decision);
        auditLog.setRuleResultsJson(ruleResultsJson);
        DecisionPolicy policy = DecisionPolicy.defaultPolicy();

        when(decisionRepository.findByLoanApplicationId(applicationId))
                .thenReturn(Optional.of(decision));
        when(auditLogRepository.findByDecisionLoanApplicationIdOrderByEventTimestampDesc(applicationId))
                .thenReturn(List.of(auditLog));
        when(policyService.getCurrentPolicy()).thenReturn(policy);

        // Act
        DecisionExplanation explanation = explanationService.explain(applicationId);

        // Assert
        assertNotNull(explanation.getRules());
        assertEquals(2, explanation.getRules().size());

        DecisionExplanation.RuleResultExplanation rule1 = explanation.getRules().get(0);
        assertEquals("MIN_INCOME", rule1.getRule());
        assertEquals("PASS", rule1.getResult());

        DecisionExplanation.RuleResultExplanation rule2 = explanation.getRules().get(1);
        assertEquals("MAX_DTI", rule2.getRule());
        assertEquals("FAIL", rule2.getResult());
        assertEquals("HIGH_DTI", rule2.getReason());
    }

    @Test
    void explain_noDecisionFound_throwsException() {
        // Arrange
        UUID applicationId = UUID.randomUUID();
        when(decisionRepository.findByLoanApplicationId(applicationId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> explanationService.explain(applicationId));
    }

    @Test
    void explain_riskAssessment_includesInterpretation() {
        // Arrange
        UUID applicationId = UUID.randomUUID();
        Decision decision = createTestDecision(applicationId, Decision.DecisionOutcome.APPROVED);
        decision.setRiskBand("A");
        DecisionPolicy policy = DecisionPolicy.defaultPolicy();

        when(decisionRepository.findByLoanApplicationId(applicationId))
                .thenReturn(Optional.of(decision));
        when(auditLogRepository.findByDecisionLoanApplicationIdOrderByEventTimestampDesc(applicationId))
                .thenReturn(Collections.emptyList());
        when(policyService.getCurrentPolicy()).thenReturn(policy);

        // Act
        DecisionExplanation explanation = explanationService.explain(applicationId);

        // Assert
        assertNotNull(explanation.getRiskAssessment());
        assertEquals("A", explanation.getRiskAssessment().getRiskBand());
        assertTrue(explanation.getRiskAssessment().getInterpretation().contains("Excellent"));
    }

    @Test
    void explainSummary_returnsSummaryWithoutRules() {
        // Arrange
        UUID applicationId = UUID.randomUUID();
        Decision decision = createTestDecision(applicationId, Decision.DecisionOutcome.APPROVED);
        DecisionPolicy policy = DecisionPolicy.defaultPolicy();

        when(decisionRepository.findByLoanApplicationId(applicationId))
                .thenReturn(Optional.of(decision));
        when(policyService.getCurrentPolicy()).thenReturn(policy);

        // Act
        DecisionExplanation explanation = explanationService.explainSummary(applicationId);

        // Assert
        assertNotNull(explanation);
        assertEquals(applicationId, explanation.getApplicationId());
        assertNull(explanation.getRules()); // Summary doesn't include rules
        assertNotNull(explanation.getRiskAssessment());
        assertNotNull(explanation.getPolicy());
    }

    @Test
    void explain_policySnapshot_includesAllFields() {
        // Arrange
        UUID applicationId = UUID.randomUUID();
        Decision decision = createTestDecision(applicationId, Decision.DecisionOutcome.APPROVED);
        DecisionPolicy policy = DecisionPolicy.defaultPolicy();

        when(decisionRepository.findByLoanApplicationId(applicationId))
                .thenReturn(Optional.of(decision));
        when(auditLogRepository.findByDecisionLoanApplicationIdOrderByEventTimestampDesc(applicationId))
                .thenReturn(Collections.emptyList());
        when(policyService.getCurrentPolicy()).thenReturn(policy);

        // Act
        DecisionExplanation explanation = explanationService.explain(applicationId);

        // Assert
        DecisionExplanation.PolicySnapshot policySnapshot = explanation.getPolicy();
        assertNotNull(policySnapshot);
        assertEquals(BigDecimal.valueOf(0.05), policySnapshot.getApproveThreshold());
        assertEquals(BigDecimal.valueOf(0.15), policySnapshot.getReviewThreshold());
        assertEquals(2, policySnapshot.getMaxSoftFailuresForApproval());
        assertTrue(policySnapshot.isHardFailDecline());
        assertEquals("default", policySnapshot.getPolicyVersion());
    }

    private Decision createTestDecision(UUID applicationId, Decision.DecisionOutcome outcome) {
        LoanApplication app = LoanApplication.builder().id(applicationId).build();
        return Decision.builder()
                .id(UUID.randomUUID())
                .loanApplication(app)
                .outcome(outcome)
                .riskBand("B")
                .probabilityOfDefault(BigDecimal.valueOf(0.03))
                .hardRuleFailures(0)
                .softRuleFailures(0)
                .decisionSummary("Test decision")
                .decidedAt(LocalDateTime.now())
                .decidedBy("SYSTEM")
                .build();
    }

    private DecisionAuditLog createTestAuditLog(Decision decision) {
        return DecisionAuditLog.builder()
                .id(UUID.randomUUID())
                .decision(decision)
                .eventType(DecisionAuditLog.EventType.DECISION_MADE)
                .eventDescription("Decision made")
                .eventTimestamp(LocalDateTime.now())
                .performedBy("SYSTEM")
                .build();
    }
}
