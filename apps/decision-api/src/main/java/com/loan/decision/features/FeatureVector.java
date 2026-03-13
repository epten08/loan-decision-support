package com.loan.decision.features;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Represents the feature vector used as input to the ML risk model.
 * This decouples the ML layer from business domain entities.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureVector {

    // Core application features
    private BigDecimal income;
    private BigDecimal loanAmount;
    private Integer loanTerm;

    // Credit profile features
    private Integer creditScore;
    private BigDecimal debtRatio;
    private Integer activeLoans;
    private BigDecimal totalExistingDebt;
    private BigDecimal monthlyDebtPayments;
    private Integer creditHistoryMonths;
    private Integer missedPaymentsLast12Months;
    private Integer defaultsLast5Years;

    // Employment features
    private String employmentStatus;

    // Derived features (calculated by FeatureExtractor)
    private BigDecimal incomeToLoanRatio;
    private BigDecimal debtBurdenIndex;
    private BigDecimal monthlyPaymentCapacity;
    private String creditScoreBand;
    private BigDecimal loanToIncomeRatio;

    /**
     * Returns the credit score band based on score ranges.
     * A: 750+, B: 700-749, C: 650-699, D: 600-649, E: <600
     */
    public static String calculateCreditScoreBand(Integer creditScore) {
        if (creditScore == null) {
            return "UNKNOWN";
        }
        if (creditScore >= 750) {
            return "A";
        } else if (creditScore >= 700) {
            return "B";
        } else if (creditScore >= 650) {
            return "C";
        } else if (creditScore >= 600) {
            return "D";
        } else {
            return "E";
        }
    }
}
