package com.loan.decision.outcomes;

import com.loan.decision.loanintake.model.LoanApplication;
import com.loan.decision.loanintake.repository.LoanApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanOutcomeServiceTest {

    @Mock
    private LoanOutcomeRepository outcomeRepository;

    @Mock
    private LoanApplicationRepository applicationRepository;

    @InjectMocks
    private LoanOutcomeService outcomeService;

    private UUID applicationId;
    private LoanApplication application;

    @BeforeEach
    void setUp() {
        applicationId = UUID.randomUUID();
        application = new LoanApplication();
        application.setId(applicationId);
    }

    @Test
    @DisplayName("recordOutcome should create outcome for valid application")
    void recordOutcome_validApplication_createsOutcome() {
        // Arrange
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(outcomeRepository.existsByLoanApplicationId(applicationId)).thenReturn(false);
        when(outcomeRepository.save(any(LoanOutcome.class))).thenAnswer(invocation -> {
            LoanOutcome outcome = invocation.getArgument(0);
            outcome.setId(UUID.randomUUID());
            return outcome;
        });

        // Act
        LoanOutcome result = outcomeService.recordOutcome(applicationId, LoanOutcome.OutcomeType.REPAID);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getOutcome()).isEqualTo(LoanOutcome.OutcomeType.REPAID);
        assertThat(result.getLoanApplication()).isEqualTo(application);
        verify(outcomeRepository).save(any(LoanOutcome.class));
    }

    @Test
    @DisplayName("recordOutcome should throw when application not found")
    void recordOutcome_applicationNotFound_throwsException() {
        // Arrange
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> outcomeService.recordOutcome(applicationId, LoanOutcome.OutcomeType.REPAID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Application not found");
    }

    @Test
    @DisplayName("recordOutcome should throw when outcome already exists")
    void recordOutcome_outcomeExists_throwsException() {
        // Arrange
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(outcomeRepository.existsByLoanApplicationId(applicationId)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> outcomeService.recordOutcome(applicationId, LoanOutcome.OutcomeType.REPAID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Outcome already recorded");
    }

    @Test
    @DisplayName("recordOutcome should save all details for delinquent outcome")
    void recordOutcome_withDetails_savesAllFields() {
        // Arrange
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(outcomeRepository.existsByLoanApplicationId(applicationId)).thenReturn(false);
        when(outcomeRepository.save(any(LoanOutcome.class))).thenAnswer(invocation -> {
            LoanOutcome outcome = invocation.getArgument(0);
            outcome.setId(UUID.randomUUID());
            return outcome;
        });

        Integer daysLate = 30;
        BigDecimal amountRecovered = new BigDecimal("5000.00");
        String recoveryStatus = "IN_PROGRESS";
        String notes = "Customer making partial payments";

        // Act
        LoanOutcome result = outcomeService.recordOutcome(
                applicationId,
                LoanOutcome.OutcomeType.DELINQUENT,
                daysLate,
                amountRecovered,
                recoveryStatus,
                notes
        );

        // Assert
        assertThat(result.getOutcome()).isEqualTo(LoanOutcome.OutcomeType.DELINQUENT);
        assertThat(result.getDaysLate()).isEqualTo(daysLate);
        assertThat(result.getAmountRecovered()).isEqualTo(amountRecovered);
        assertThat(result.getRecoveryStatus()).isEqualTo(recoveryStatus);
        assertThat(result.getNotes()).isEqualTo(notes);
    }

    @Test
    @DisplayName("getOutcome should return outcome when exists")
    void getOutcome_exists_returnsOutcome() {
        // Arrange
        LoanOutcome outcome = LoanOutcome.builder()
                .id(UUID.randomUUID())
                .loanApplication(application)
                .outcome(LoanOutcome.OutcomeType.REPAID)
                .build();

        when(outcomeRepository.findByLoanApplicationId(applicationId)).thenReturn(Optional.of(outcome));

        // Act
        LoanOutcome result = outcomeService.getOutcome(applicationId);

        // Assert
        assertThat(result).isEqualTo(outcome);
    }

    @Test
    @DisplayName("getOutcome should throw when not found")
    void getOutcome_notFound_throwsException() {
        // Arrange
        when(outcomeRepository.findByLoanApplicationId(applicationId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> outcomeService.getOutcome(applicationId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Outcome not found");
    }

    @Test
    @DisplayName("hasOutcome should return true when outcome exists")
    void hasOutcome_exists_returnsTrue() {
        // Arrange
        when(outcomeRepository.existsByLoanApplicationId(applicationId)).thenReturn(true);

        // Act
        boolean result = outcomeService.hasOutcome(applicationId);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("hasOutcome should return false when outcome does not exist")
    void hasOutcome_notExists_returnsFalse() {
        // Arrange
        when(outcomeRepository.existsByLoanApplicationId(applicationId)).thenReturn(false);

        // Act
        boolean result = outcomeService.hasOutcome(applicationId);

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("getOutcomesByType should return matching outcomes")
    void getOutcomesByType_returnsMatchingOutcomes() {
        // Arrange
        List<LoanOutcome> outcomes = List.of(
                LoanOutcome.builder().outcome(LoanOutcome.OutcomeType.DEFAULTED).build(),
                LoanOutcome.builder().outcome(LoanOutcome.OutcomeType.DEFAULTED).build()
        );
        when(outcomeRepository.findByOutcome(LoanOutcome.OutcomeType.DEFAULTED)).thenReturn(outcomes);

        // Act
        List<LoanOutcome> result = outcomeService.getOutcomesByType(LoanOutcome.OutcomeType.DEFAULTED);

        // Assert
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("getStatistics should calculate correct statistics")
    void getStatistics_calculatesCorrectly() {
        // Arrange
        when(outcomeRepository.count()).thenReturn(100L);
        when(outcomeRepository.countByOutcome(LoanOutcome.OutcomeType.REPAID)).thenReturn(70L);
        when(outcomeRepository.countByOutcome(LoanOutcome.OutcomeType.DEFAULTED)).thenReturn(10L);
        when(outcomeRepository.countByOutcome(LoanOutcome.OutcomeType.DELINQUENT)).thenReturn(5L);
        when(outcomeRepository.countByOutcome(LoanOutcome.OutcomeType.WRITTEN_OFF)).thenReturn(5L);
        when(outcomeRepository.countByOutcome(LoanOutcome.OutcomeType.PERFORMING)).thenReturn(8L);
        when(outcomeRepository.countByOutcome(LoanOutcome.OutcomeType.PREPAID)).thenReturn(2L);

        // Act
        LoanOutcomeService.OutcomeStatistics stats = outcomeService.getStatistics();

        // Assert
        assertThat(stats.getTotalOutcomes()).isEqualTo(100);
        assertThat(stats.getRepaid()).isEqualTo(70);
        assertThat(stats.getDefaulted()).isEqualTo(10);
        assertThat(stats.getDelinquent()).isEqualTo(5);
        assertThat(stats.getWrittenOff()).isEqualTo(5);
        assertThat(stats.getPerforming()).isEqualTo(8);
        assertThat(stats.getPrepaid()).isEqualTo(2);
        assertThat(stats.getDefaultRate()).isEqualTo(0.15); // (10 + 5) / 100
    }

    @Test
    @DisplayName("getStatistics should handle zero outcomes")
    void getStatistics_zeroOutcomes_returnsZeroRate() {
        // Arrange
        when(outcomeRepository.count()).thenReturn(0L);
        when(outcomeRepository.countByOutcome(any())).thenReturn(0L);

        // Act
        LoanOutcomeService.OutcomeStatistics stats = outcomeService.getStatistics();

        // Assert
        assertThat(stats.getTotalOutcomes()).isEqualTo(0);
        assertThat(stats.getDefaultRate()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("updateOutcome should update existing outcome")
    void updateOutcome_existingOutcome_updates() {
        // Arrange
        LoanOutcome existing = LoanOutcome.builder()
                .id(UUID.randomUUID())
                .loanApplication(application)
                .outcome(LoanOutcome.OutcomeType.DELINQUENT)
                .build();

        when(outcomeRepository.findByLoanApplicationId(applicationId)).thenReturn(Optional.of(existing));
        when(outcomeRepository.save(any(LoanOutcome.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        LoanOutcome result = outcomeService.updateOutcome(
                applicationId,
                LoanOutcome.OutcomeType.DEFAULTED,
                "Customer stopped paying"
        );

        // Assert
        assertThat(result.getOutcome()).isEqualTo(LoanOutcome.OutcomeType.DEFAULTED);
        assertThat(result.getNotes()).isEqualTo("Customer stopped paying");
        verify(outcomeRepository).save(existing);
    }

    @Test
    @DisplayName("updateOutcome should throw when outcome not found")
    void updateOutcome_notFound_throwsException() {
        // Arrange
        when(outcomeRepository.findByLoanApplicationId(applicationId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> outcomeService.updateOutcome(
                applicationId,
                LoanOutcome.OutcomeType.DEFAULTED,
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Outcome not found");
    }
}
