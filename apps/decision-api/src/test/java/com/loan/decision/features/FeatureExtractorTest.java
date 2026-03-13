package com.loan.decision.features;

import com.loan.decision.creditprofile.model.CreditProfile;
import com.loan.decision.loanintake.model.Applicant;
import com.loan.decision.loanintake.model.LoanApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FeatureExtractorTest {

    private FeatureExtractor featureExtractor;

    @BeforeEach
    void setUp() {
        featureExtractor = new FeatureExtractor();
    }

    @Test
    void extract_withFullData_extractsAllFeatures() {
        // Arrange
        LoanApplication application = createTestApplication();
        CreditProfile creditProfile = createTestCreditProfile(application);

        // Act
        FeatureVector features = featureExtractor.extract(application, creditProfile);

        // Assert
        assertNotNull(features);
        assertEquals(BigDecimal.valueOf(5000), features.getIncome());
        assertEquals(BigDecimal.valueOf(10000), features.getLoanAmount());
        assertEquals(24, features.getLoanTerm());
        assertEquals(700, features.getCreditScore());
        assertEquals("EMPLOYED", features.getEmploymentStatus());
    }

    @Test
    void extract_calculatesIncomeToLoanRatio() {
        // Arrange
        LoanApplication application = createTestApplication();
        CreditProfile creditProfile = createTestCreditProfile(application);

        // Act
        FeatureVector features = featureExtractor.extract(application, creditProfile);

        // Assert
        // income (5000) / loanAmount (10000) = 0.5
        assertNotNull(features.getIncomeToLoanRatio());
        assertEquals(0, BigDecimal.valueOf(0.5).compareTo(features.getIncomeToLoanRatio()));
    }

    @Test
    void extract_calculatesLoanToIncomeRatio() {
        // Arrange
        LoanApplication application = createTestApplication();
        CreditProfile creditProfile = createTestCreditProfile(application);

        // Act
        FeatureVector features = featureExtractor.extract(application, creditProfile);

        // Assert
        // loanAmount (10000) / income (5000) = 2.0
        assertNotNull(features.getLoanToIncomeRatio());
        assertEquals(0, BigDecimal.valueOf(2).compareTo(features.getLoanToIncomeRatio()));
    }

    @Test
    void extract_calculatesDebtBurdenIndex() {
        // Arrange
        LoanApplication application = createTestApplication();
        CreditProfile creditProfile = createTestCreditProfile(application);

        // Act
        FeatureVector features = featureExtractor.extract(application, creditProfile);

        // Assert
        // (monthlyDebtPayments (200) + loanAmount/term (10000/24)) / income (5000)
        // = (200 + 416.67) / 5000 = ~0.123
        assertNotNull(features.getDebtBurdenIndex());
        assertTrue(features.getDebtBurdenIndex().compareTo(BigDecimal.valueOf(0.1)) > 0);
        assertTrue(features.getDebtBurdenIndex().compareTo(BigDecimal.valueOf(0.2)) < 0);
    }

    @Test
    void extract_calculatesMonthlyPaymentCapacity() {
        // Arrange
        LoanApplication application = createTestApplication();
        CreditProfile creditProfile = createTestCreditProfile(application);

        // Act
        FeatureVector features = featureExtractor.extract(application, creditProfile);

        // Assert
        // income (5000) - monthlyDebtPayments (200) = 4800
        assertNotNull(features.getMonthlyPaymentCapacity());
        assertEquals(0, BigDecimal.valueOf(4800).compareTo(features.getMonthlyPaymentCapacity()));
    }

    @Test
    void extract_calculatesCreditScoreBand() {
        // Arrange
        LoanApplication application = createTestApplication();
        CreditProfile creditProfile = createTestCreditProfile(application);
        creditProfile.setCreditScore(700);

        // Act
        FeatureVector features = featureExtractor.extract(application, creditProfile);

        // Assert - 700 should be band B
        assertEquals("B", features.getCreditScoreBand());
    }

    @Test
    void extract_withNullCreditProfile_handlesGracefully() {
        // Arrange
        LoanApplication application = createTestApplication();

        // Act
        FeatureVector features = featureExtractor.extract(application, null);

        // Assert
        assertNotNull(features);
        assertEquals(BigDecimal.valueOf(5000), features.getIncome());
        assertEquals(BigDecimal.valueOf(10000), features.getLoanAmount());
        assertNull(features.getCreditScore());
        assertNull(features.getDebtRatio());
        assertEquals("UNKNOWN", features.getCreditScoreBand());
    }

    @Test
    void extract_withZeroLoanAmount_handlesGracefully() {
        // Arrange
        LoanApplication application = createTestApplication();
        application.setRequestedAmount(BigDecimal.ZERO);
        CreditProfile creditProfile = createTestCreditProfile(application);

        // Act
        FeatureVector features = featureExtractor.extract(application, creditProfile);

        // Assert
        assertNull(features.getIncomeToLoanRatio());
    }

    @Test
    void creditScoreBand_variousScores_returnsCorrectBand() {
        assertEquals("A", FeatureVector.calculateCreditScoreBand(800));
        assertEquals("A", FeatureVector.calculateCreditScoreBand(750));
        assertEquals("B", FeatureVector.calculateCreditScoreBand(749));
        assertEquals("B", FeatureVector.calculateCreditScoreBand(700));
        assertEquals("C", FeatureVector.calculateCreditScoreBand(699));
        assertEquals("C", FeatureVector.calculateCreditScoreBand(650));
        assertEquals("D", FeatureVector.calculateCreditScoreBand(649));
        assertEquals("D", FeatureVector.calculateCreditScoreBand(600));
        assertEquals("E", FeatureVector.calculateCreditScoreBand(599));
        assertEquals("E", FeatureVector.calculateCreditScoreBand(300));
        assertEquals("UNKNOWN", FeatureVector.calculateCreditScoreBand(null));
    }

    @Test
    void extract_preservesCreditProfileFields() {
        // Arrange
        LoanApplication application = createTestApplication();
        CreditProfile creditProfile = createTestCreditProfile(application);
        creditProfile.setMissedPaymentsLast12Months(2);
        creditProfile.setDefaultsLast5Years(1);
        creditProfile.setCreditHistoryMonths(48);

        // Act
        FeatureVector features = featureExtractor.extract(application, creditProfile);

        // Assert
        assertEquals(2, features.getMissedPaymentsLast12Months());
        assertEquals(1, features.getDefaultsLast5Years());
        assertEquals(48, features.getCreditHistoryMonths());
    }

    private LoanApplication createTestApplication() {
        Applicant applicant = Applicant.builder()
                .id(UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .nationalId("12345")
                .employmentStatus("EMPLOYED")
                .build();

        return LoanApplication.builder()
                .id(UUID.randomUUID())
                .applicant(applicant)
                .requestedAmount(BigDecimal.valueOf(10000))
                .termMonths(24)
                .monthlyIncome(BigDecimal.valueOf(5000))
                .monthlyExpenses(BigDecimal.valueOf(2000))
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
                .existingLoanCount(1)
                .totalExistingDebt(BigDecimal.valueOf(5000))
                .monthlyDebtPayments(BigDecimal.valueOf(200))
                .creditHistoryMonths(36)
                .missedPaymentsLast12Months(0)
                .defaultsLast5Years(0)
                .build();
    }
}
