package com.loan.decision.riskadapter.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskAssessmentRequest {

    private BigDecimal monthlyIncome;
    private BigDecimal monthlyExpenses;
    private BigDecimal requestedAmount;
    private Integer termMonths;
    private Integer creditScore;
    private BigDecimal debtToIncomeRatio;
    private Integer existingLoanCount;
    private BigDecimal totalExistingDebt;
    private Integer creditHistoryMonths;
    private Integer missedPaymentsLast12Months;
    private Integer defaultsLast5Years;
    private String employmentStatus;
}
