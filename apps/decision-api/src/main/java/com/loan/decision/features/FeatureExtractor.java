package com.loan.decision.features;

import com.loan.decision.creditprofile.model.CreditProfile;
import com.loan.decision.loanintake.model.LoanApplication;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Extracts ML features from loan application domain objects.
 * This layer provides model flexibility and decouples ML from business entities.
 */
@Component
@Slf4j
public class FeatureExtractor {

    private static final int SCALE = 6;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    /**
     * Extracts a feature vector from the loan application and credit profile.
     *
     * @param application the loan application
     * @param creditProfile the credit profile (may be null)
     * @return the feature vector for ML model input
     */
    public FeatureVector extract(LoanApplication application, CreditProfile creditProfile) {
        log.debug("Extracting features for application: {}", application.getId());

        BigDecimal income = application.getMonthlyIncome();
        BigDecimal loanAmount = application.getRequestedAmount();
        Integer loanTerm = application.getTermMonths();

        // Extract credit profile features
        Integer creditScore = null;
        BigDecimal debtRatio = null;
        Integer activeLoans = null;
        BigDecimal totalExistingDebt = null;
        BigDecimal monthlyDebtPayments = null;
        Integer creditHistoryMonths = null;
        Integer missedPayments = null;
        Integer defaults = null;

        if (creditProfile != null) {
            creditScore = creditProfile.getCreditScore();
            debtRatio = creditProfile.getDebtToIncomeRatio();
            activeLoans = creditProfile.getExistingLoanCount();
            totalExistingDebt = creditProfile.getTotalExistingDebt();
            monthlyDebtPayments = creditProfile.getMonthlyDebtPayments();
            creditHistoryMonths = creditProfile.getCreditHistoryMonths();
            missedPayments = creditProfile.getMissedPaymentsLast12Months();
            defaults = creditProfile.getDefaultsLast5Years();
        }

        // Calculate derived features
        BigDecimal incomeToLoanRatio = calculateIncomeToLoanRatio(income, loanAmount);
        BigDecimal loanToIncomeRatio = calculateLoanToIncomeRatio(loanAmount, income);
        BigDecimal debtBurdenIndex = calculateDebtBurdenIndex(monthlyDebtPayments, income, loanAmount, loanTerm);
        BigDecimal monthlyPaymentCapacity = calculateMonthlyPaymentCapacity(income, monthlyDebtPayments);
        String creditScoreBand = FeatureVector.calculateCreditScoreBand(creditScore);

        // Employment status
        String employmentStatus = null;
        if (application.getApplicant() != null) {
            employmentStatus = application.getApplicant().getEmploymentStatus();
        }

        FeatureVector vector = FeatureVector.builder()
                // Core features
                .income(income)
                .loanAmount(loanAmount)
                .loanTerm(loanTerm)
                // Credit features
                .creditScore(creditScore)
                .debtRatio(debtRatio)
                .activeLoans(activeLoans)
                .totalExistingDebt(totalExistingDebt)
                .monthlyDebtPayments(monthlyDebtPayments)
                .creditHistoryMonths(creditHistoryMonths)
                .missedPaymentsLast12Months(missedPayments)
                .defaultsLast5Years(defaults)
                // Employment
                .employmentStatus(employmentStatus)
                // Derived features
                .incomeToLoanRatio(incomeToLoanRatio)
                .loanToIncomeRatio(loanToIncomeRatio)
                .debtBurdenIndex(debtBurdenIndex)
                .monthlyPaymentCapacity(monthlyPaymentCapacity)
                .creditScoreBand(creditScoreBand)
                .build();

        log.debug("Extracted feature vector with {} derived features", 5);
        return vector;
    }

    /**
     * Calculates income to loan ratio.
     * Higher values indicate better affordability.
     */
    private BigDecimal calculateIncomeToLoanRatio(BigDecimal income, BigDecimal loanAmount) {
        if (income == null || loanAmount == null || loanAmount.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return income.divide(loanAmount, SCALE, ROUNDING);
    }

    /**
     * Calculates loan to income ratio (how many months of income equals the loan).
     * Lower values are better.
     */
    private BigDecimal calculateLoanToIncomeRatio(BigDecimal loanAmount, BigDecimal income) {
        if (loanAmount == null || income == null || income.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return loanAmount.divide(income, SCALE, ROUNDING);
    }

    /**
     * Calculates the debt burden index.
     * This estimates total monthly debt obligations as a percentage of income.
     * Includes existing debt payments plus estimated new loan payment.
     */
    private BigDecimal calculateDebtBurdenIndex(BigDecimal monthlyDebtPayments,
                                                  BigDecimal income,
                                                  BigDecimal loanAmount,
                                                  Integer loanTerm) {
        if (income == null || income.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        BigDecimal existingPayments = monthlyDebtPayments != null ? monthlyDebtPayments : BigDecimal.ZERO;

        // Estimate monthly payment for new loan (simple division, ignoring interest)
        BigDecimal estimatedNewPayment = BigDecimal.ZERO;
        if (loanAmount != null && loanTerm != null && loanTerm > 0) {
            estimatedNewPayment = loanAmount.divide(BigDecimal.valueOf(loanTerm), SCALE, ROUNDING);
        }

        BigDecimal totalMonthlyDebt = existingPayments.add(estimatedNewPayment);
        return totalMonthlyDebt.divide(income, SCALE, ROUNDING);
    }

    /**
     * Calculates monthly payment capacity.
     * This is income minus existing debt payments.
     */
    private BigDecimal calculateMonthlyPaymentCapacity(BigDecimal income, BigDecimal monthlyDebtPayments) {
        if (income == null) {
            return null;
        }
        BigDecimal payments = monthlyDebtPayments != null ? monthlyDebtPayments : BigDecimal.ZERO;
        return income.subtract(payments);
    }
}
