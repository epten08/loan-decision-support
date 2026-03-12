package com.loan.decision.riskadapter.controller.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("monthly_income")
    private BigDecimal monthlyIncome;

    @JsonProperty("monthly_expenses")
    private BigDecimal monthlyExpenses;

    @JsonProperty("requested_amount")
    private BigDecimal requestedAmount;

    @JsonProperty("term_months")
    private Integer termMonths;

    @JsonProperty("credit_score")
    private Integer creditScore;

    @JsonProperty("debt_to_income_ratio")
    private BigDecimal debtToIncomeRatio;

    @JsonProperty("existing_loan_count")
    private Integer existingLoanCount;

    @JsonProperty("total_existing_debt")
    private BigDecimal totalExistingDebt;

    @JsonProperty("credit_history_months")
    private Integer creditHistoryMonths;

    @JsonProperty("missed_payments_last_12_months")
    private Integer missedPaymentsLast12Months;

    @JsonProperty("defaults_last_5_years")
    private Integer defaultsLast5Years;

    @JsonProperty("employment_status")
    private String employmentStatus;
}
