package com.loan.decision.evaluation;

import com.loan.decision.creditprofile.model.CreditProfile;
import com.loan.decision.creditprofile.repository.CreditProfileRepository;
import com.loan.decision.decisioning.model.Decision;
import com.loan.decision.decisioning.repository.DecisionRepository;
import com.loan.decision.decisioning.service.DecisionAggregatorService;
import com.loan.decision.loanintake.model.Applicant;
import com.loan.decision.loanintake.model.LoanApplication;
import com.loan.decision.loanintake.repository.LoanApplicationRepository;
import com.loan.decision.riskadapter.model.RiskAssessment;
import com.loan.decision.riskadapter.service.RiskAdapterService;
import com.loan.decision.rules.model.RuleDefinition;
import com.loan.decision.rules.model.RuleResult;
import com.loan.decision.rules.service.RuleEngineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvaluationOrchestratorTest {

    @Mock
    private LoanApplicationRepository loanApplicationRepository;

    @Mock
    private CreditProfileRepository creditProfileRepository;

    @Mock
    private DecisionRepository decisionRepository;

    @Mock
    private RuleEngineService ruleEngineService;

    @Mock
    private RiskAdapterService riskAdapterService;

    @Mock
    private DecisionAggregatorService decisionAggregatorService;

    private EvaluationOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new EvaluationOrchestrator(
                loanApplicationRepository,
                creditProfileRepository,
                decisionRepository,
                ruleEngineService,
                riskAdapterService,
                decisionAggregatorService
        );
    }

    @Test
    void evaluate_existingDecision_returnsFromCache() {
        // Arrange
        UUID applicationId = UUID.randomUUID();
        Decision existingDecision = createTestDecision(applicationId, Decision.DecisionOutcome.APPROVED);

        when(decisionRepository.existsByLoanApplicationId(applicationId)).thenReturn(true);
        when(decisionRepository.findByLoanApplicationId(applicationId))
                .thenReturn(Optional.of(existingDecision));

        // Act
        EvaluationResult result = orchestrator.evaluate(applicationId);

        // Assert
        assertTrue(result.isFromCache());
        assertEquals(Decision.DecisionOutcome.APPROVED, result.getOutcome());
        verify(ruleEngineService, never()).evaluateApplication(any(), any());
        verify(riskAdapterService, never()).assessRisk(any(), any());
    }

    @Test
    void evaluate_newApplication_runsFullPipeline() {
        // Arrange
        UUID applicationId = UUID.randomUUID();
        LoanApplication application = createTestApplication(applicationId);
        CreditProfile creditProfile = createTestCreditProfile(application);
        List<RuleResult> ruleResults = createPassingRuleResults();
        RiskAssessment riskAssessment = createLowRiskAssessment();
        Decision decision = createTestDecision(applicationId, Decision.DecisionOutcome.APPROVED);

        when(decisionRepository.existsByLoanApplicationId(applicationId)).thenReturn(false);
        when(loanApplicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(loanApplicationRepository.save(any())).thenReturn(application);
        when(creditProfileRepository.findByLoanApplicationId(applicationId))
                .thenReturn(Optional.of(creditProfile));
        when(ruleEngineService.evaluateApplication(application, creditProfile)).thenReturn(ruleResults);
        when(ruleEngineService.hasHardFailure(ruleResults)).thenReturn(false);
        when(riskAdapterService.assessRisk(application, creditProfile)).thenReturn(riskAssessment);
        when(decisionAggregatorService.aggregateDecision(application, ruleResults, riskAssessment))
                .thenReturn(decision);

        // Act
        EvaluationResult result = orchestrator.evaluate(applicationId);

        // Assert
        assertFalse(result.isFromCache());
        assertEquals(Decision.DecisionOutcome.APPROVED, result.getOutcome());
        verify(ruleEngineService).evaluateApplication(application, creditProfile);
        verify(riskAdapterService).assessRisk(application, creditProfile);
        verify(decisionAggregatorService).aggregateDecision(application, ruleResults, riskAssessment);
    }

    @Test
    void evaluate_hardFailure_skipsRiskEngine() {
        // Arrange
        UUID applicationId = UUID.randomUUID();
        LoanApplication application = createTestApplication(applicationId);
        CreditProfile creditProfile = createTestCreditProfile(application);
        List<RuleResult> ruleResults = createHardFailureRuleResults();
        Decision decision = createTestDecision(applicationId, Decision.DecisionOutcome.DECLINED);

        when(decisionRepository.existsByLoanApplicationId(applicationId)).thenReturn(false);
        when(loanApplicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(loanApplicationRepository.save(any())).thenReturn(application);
        when(creditProfileRepository.findByLoanApplicationId(applicationId))
                .thenReturn(Optional.of(creditProfile));
        when(ruleEngineService.evaluateApplication(application, creditProfile)).thenReturn(ruleResults);
        when(ruleEngineService.hasHardFailure(ruleResults)).thenReturn(true);
        when(decisionAggregatorService.aggregateDecision(eq(application), eq(ruleResults), any()))
                .thenReturn(decision);

        // Act
        EvaluationResult result = orchestrator.evaluate(applicationId);

        // Assert
        assertEquals(Decision.DecisionOutcome.DECLINED, result.getOutcome());
        verify(riskAdapterService, never()).assessRisk(any(), any());
    }

    @Test
    void evaluate_forceReevaluation_ignoresExistingDecision() {
        // Arrange
        UUID applicationId = UUID.randomUUID();
        LoanApplication application = createTestApplication(applicationId);
        CreditProfile creditProfile = createTestCreditProfile(application);
        List<RuleResult> ruleResults = createPassingRuleResults();
        RiskAssessment riskAssessment = createLowRiskAssessment();
        Decision decision = createTestDecision(applicationId, Decision.DecisionOutcome.APPROVED);

        EvaluationRequest request = EvaluationRequest.builder()
                .applicationId(applicationId)
                .forceReevaluation(true)
                .build();

        when(loanApplicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(loanApplicationRepository.save(any())).thenReturn(application);
        when(creditProfileRepository.findByLoanApplicationId(applicationId))
                .thenReturn(Optional.of(creditProfile));
        when(ruleEngineService.evaluateApplication(application, creditProfile)).thenReturn(ruleResults);
        when(ruleEngineService.hasHardFailure(ruleResults)).thenReturn(false);
        when(riskAdapterService.assessRisk(application, creditProfile)).thenReturn(riskAssessment);
        when(decisionAggregatorService.aggregateDecision(application, ruleResults, riskAssessment))
                .thenReturn(decision);

        // Act
        EvaluationResult result = orchestrator.evaluate(request);

        // Assert
        assertFalse(result.isFromCache());
        verify(decisionRepository, never()).existsByLoanApplicationId(applicationId);
        verify(ruleEngineService).evaluateApplication(application, creditProfile);
    }

    @Test
    void evaluate_applicationNotFound_throwsException() {
        // Arrange
        UUID applicationId = UUID.randomUUID();

        when(decisionRepository.existsByLoanApplicationId(applicationId)).thenReturn(false);
        when(loanApplicationRepository.findById(applicationId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> orchestrator.evaluate(applicationId));
    }

    @Test
    void getDecision_existing_returnsResult() {
        // Arrange
        UUID applicationId = UUID.randomUUID();
        Decision decision = createTestDecision(applicationId, Decision.DecisionOutcome.APPROVED);

        when(decisionRepository.findByLoanApplicationId(applicationId))
                .thenReturn(Optional.of(decision));

        // Act
        EvaluationResult result = orchestrator.getDecision(applicationId);

        // Assert
        assertTrue(result.isFromCache());
        assertEquals(Decision.DecisionOutcome.APPROVED, result.getOutcome());
    }

    @Test
    void getDecision_notFound_throwsException() {
        // Arrange
        UUID applicationId = UUID.randomUUID();

        when(decisionRepository.findByLoanApplicationId(applicationId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> orchestrator.getDecision(applicationId));
    }

    private LoanApplication createTestApplication(UUID applicationId) {
        Applicant applicant = Applicant.builder()
                .id(UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .nationalId("12345")
                .employmentStatus("EMPLOYED")
                .build();

        return LoanApplication.builder()
                .id(applicationId)
                .applicant(applicant)
                .requestedAmount(BigDecimal.valueOf(10000))
                .termMonths(24)
                .monthlyIncome(BigDecimal.valueOf(5000))
                .loanPurpose("Personal")
                .status(LoanApplication.ApplicationStatus.PENDING)
                .build();
    }

    private CreditProfile createTestCreditProfile(LoanApplication application) {
        return CreditProfile.builder()
                .id(UUID.randomUUID())
                .loanApplication(application)
                .creditScore(700)
                .debtToIncomeRatio(BigDecimal.valueOf(0.25))
                .build();
    }

    private Decision createTestDecision(UUID applicationId, Decision.DecisionOutcome outcome) {
        LoanApplication app = LoanApplication.builder().id(applicationId).build();
        return Decision.builder()
                .id(UUID.randomUUID())
                .loanApplication(app)
                .outcome(outcome)
                .riskBand("B")
                .probabilityOfDefault(BigDecimal.valueOf(0.05))
                .hardRuleFailures(0)
                .softRuleFailures(0)
                .decisionSummary("Test decision")
                .decidedAt(LocalDateTime.now())
                .build();
    }

    private List<RuleResult> createPassingRuleResults() {
        return List.of(
                RuleResult.builder()
                        .ruleCode("MIN_INCOME")
                        .passed(true)
                        .severity(RuleDefinition.RuleSeverity.HARD)
                        .build(),
                RuleResult.builder()
                        .ruleCode("MIN_CREDIT_SCORE")
                        .passed(true)
                        .severity(RuleDefinition.RuleSeverity.HARD)
                        .build()
        );
    }

    private List<RuleResult> createHardFailureRuleResults() {
        return List.of(
                RuleResult.builder()
                        .ruleCode("MIN_INCOME")
                        .passed(false)
                        .severity(RuleDefinition.RuleSeverity.HARD)
                        .reason("INSUFFICIENT_INCOME")
                        .build()
        );
    }

    private RiskAssessment createLowRiskAssessment() {
        return RiskAssessment.builder()
                .id(UUID.randomUUID())
                .probabilityOfDefault(BigDecimal.valueOf(0.03))
                .riskBand(RiskAssessment.RiskBand.A)
                .confidence(BigDecimal.valueOf(0.95))
                .build();
    }
}
