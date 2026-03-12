package com.loan.decision.governance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loan.decision.decisioning.model.Decision;
import com.loan.decision.decisioning.repository.DecisionRepository;
import com.loan.decision.governance.model.DecisionAuditLog;
import com.loan.decision.governance.repository.DecisionAuditLogRepository;
import com.loan.decision.loanintake.model.Applicant;
import com.loan.decision.loanintake.model.LoanApplication;
import com.loan.decision.riskadapter.model.RiskAssessment;
import com.loan.decision.rules.model.RuleDefinition;
import com.loan.decision.rules.model.RuleResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DecisionPersistenceServiceTest {

    @Mock
    private DecisionRepository decisionRepository;

    @Mock
    private DecisionAuditLogRepository auditLogRepository;

    private DecisionPersistenceService persistenceService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        persistenceService = new DecisionPersistenceService(
                decisionRepository,
                auditLogRepository,
                objectMapper
        );
    }

    @Test
    void saveDecision_savesDecisionAndAuditLog() {
        // Arrange
        LoanApplication application = createTestApplication();
        List<RuleResult> ruleResults = createTestRuleResults();
        RiskAssessment riskAssessment = createTestRiskAssessment();
        List<String> reasonCodes = Arrays.asList("LOW_CREDIT_SCORE");
        String summary = "Test decision";

        when(decisionRepository.save(any(Decision.class))).thenAnswer(inv -> {
            Decision d = inv.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });
        when(auditLogRepository.save(any(DecisionAuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Decision result = persistenceService.saveDecision(
                application,
                Decision.DecisionOutcome.DECLINED,
                ruleResults,
                riskAssessment,
                reasonCodes,
                summary
        );

        // Assert
        assertNotNull(result);
        assertEquals(Decision.DecisionOutcome.DECLINED, result.getOutcome());
        assertEquals("LOW_CREDIT_SCORE", result.getReasonCodes());
        assertEquals("B", result.getRiskBand());
        assertEquals(1, result.getHardRuleFailures());
        assertEquals(1, result.getSoftRuleFailures());

        verify(decisionRepository).save(any(Decision.class));
        verify(auditLogRepository).save(any(DecisionAuditLog.class));
    }

    @Test
    void saveDecision_storesJsonSnapshots() {
        // Arrange
        LoanApplication application = createTestApplication();
        List<RuleResult> ruleResults = createTestRuleResults();
        RiskAssessment riskAssessment = createTestRiskAssessment();

        when(decisionRepository.save(any(Decision.class))).thenAnswer(inv -> {
            Decision d = inv.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });

        ArgumentCaptor<DecisionAuditLog> auditCaptor = ArgumentCaptor.forClass(DecisionAuditLog.class);
        when(auditLogRepository.save(auditCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        persistenceService.saveDecision(
                application,
                Decision.DecisionOutcome.APPROVED,
                ruleResults,
                riskAssessment,
                List.of(),
                "Approved"
        );

        // Assert
        DecisionAuditLog capturedLog = auditCaptor.getValue();
        assertNotNull(capturedLog.getRuleResultsJson());
        assertNotNull(capturedLog.getRiskAssessmentJson());
        assertTrue(capturedLog.getRuleResultsJson().contains("MIN_CREDIT_SCORE"));
        assertTrue(capturedLog.getRiskAssessmentJson().contains("probabilityOfDefault"));
    }

    @Test
    void saveDecision_storesSummaryStrings() {
        // Arrange
        LoanApplication application = createTestApplication();
        List<RuleResult> ruleResults = createTestRuleResults();
        RiskAssessment riskAssessment = createTestRiskAssessment();

        when(decisionRepository.save(any(Decision.class))).thenAnswer(inv -> {
            Decision d = inv.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });

        ArgumentCaptor<DecisionAuditLog> auditCaptor = ArgumentCaptor.forClass(DecisionAuditLog.class);
        when(auditLogRepository.save(auditCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        persistenceService.saveDecision(
                application,
                Decision.DecisionOutcome.APPROVED,
                ruleResults,
                riskAssessment,
                List.of(),
                "Approved"
        );

        // Assert
        DecisionAuditLog capturedLog = auditCaptor.getValue();
        assertNotNull(capturedLog.getRuleEvaluationSummary());
        assertNotNull(capturedLog.getRiskAssessmentSummary());
        assertTrue(capturedLog.getRuleEvaluationSummary().contains("MIN_CREDIT_SCORE:FAIL"));
        assertTrue(capturedLog.getRiskAssessmentSummary().contains("Band:B"));
    }

    @Test
    void logDecisionOverride_createsOverrideAuditEntry() {
        // Arrange
        Decision decision = Decision.builder()
                .id(UUID.randomUUID())
                .outcome(Decision.DecisionOutcome.APPROVED)
                .build();

        ArgumentCaptor<DecisionAuditLog> auditCaptor = ArgumentCaptor.forClass(DecisionAuditLog.class);
        when(auditLogRepository.save(auditCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        persistenceService.logDecisionOverride(
                decision,
                Decision.DecisionOutcome.DECLINED,
                "admin@example.com",
                "Manual override based on additional documentation"
        );

        // Assert
        DecisionAuditLog capturedLog = auditCaptor.getValue();
        assertEquals(DecisionAuditLog.EventType.DECISION_OVERRIDDEN, capturedLog.getEventType());
        assertEquals("DECLINED", capturedLog.getPreviousState());
        assertEquals("APPROVED", capturedLog.getNewState());
        assertEquals("admin@example.com", capturedLog.getPerformedBy());
    }

    private LoanApplication createTestApplication() {
        Applicant applicant = Applicant.builder()
                .id(UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .nationalId("12345")
                .build();

        return LoanApplication.builder()
                .id(UUID.randomUUID())
                .applicant(applicant)
                .requestedAmount(BigDecimal.valueOf(10000))
                .termMonths(24)
                .monthlyIncome(BigDecimal.valueOf(5000))
                .loanPurpose("Personal")
                .status(LoanApplication.ApplicationStatus.PENDING)
                .build();
    }

    private List<RuleResult> createTestRuleResults() {
        return Arrays.asList(
                RuleResult.builder()
                        .ruleCode("MIN_CREDIT_SCORE")
                        .passed(false)
                        .severity(RuleDefinition.RuleSeverity.HARD)
                        .reason("LOW_CREDIT_SCORE")
                        .evaluatedCondition("credit_score < 600")
                        .actualValue(550)
                        .build(),
                RuleResult.builder()
                        .ruleCode("MAX_MISSED_PAYMENTS")
                        .passed(false)
                        .severity(RuleDefinition.RuleSeverity.SOFT)
                        .reason("PAYMENT_HISTORY_CONCERN")
                        .evaluatedCondition("missed_payments > 3")
                        .actualValue(4)
                        .build(),
                RuleResult.builder()
                        .ruleCode("MIN_INCOME")
                        .passed(true)
                        .severity(RuleDefinition.RuleSeverity.HARD)
                        .evaluatedCondition("monthly_income < 500")
                        .actualValue(5000)
                        .build()
        );
    }

    private RiskAssessment createTestRiskAssessment() {
        return RiskAssessment.builder()
                .id(UUID.randomUUID())
                .probabilityOfDefault(BigDecimal.valueOf(0.08))
                .riskBand(RiskAssessment.RiskBand.B)
                .confidence(BigDecimal.valueOf(0.85))
                .modelVersion("heuristic-v1")
                .build();
    }
}
