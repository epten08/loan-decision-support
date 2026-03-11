package com.loan.decision.riskadapter.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loan.decision.creditprofile.model.CreditProfile;
import com.loan.decision.loanintake.model.LoanApplication;
import com.loan.decision.riskadapter.controller.dto.RiskAssessmentRequest;
import com.loan.decision.riskadapter.controller.dto.RiskAssessmentResponse;
import com.loan.decision.riskadapter.model.RiskAssessment;
import com.loan.decision.riskadapter.repository.RiskAssessmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiskAdapterService {

    private final RiskEngineClient riskEngineClient;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public RiskAssessment assessRisk(LoanApplication application, CreditProfile creditProfile) {
        log.info("Assessing risk for application: {}", application.getId());

        // Check if assessment already exists
        if (riskAssessmentRepository.existsByLoanApplicationId(application.getId())) {
            log.info("Risk assessment already exists for application: {}", application.getId());
            return riskAssessmentRepository.findByLoanApplicationId(application.getId())
                    .orElseThrow();
        }

        RiskAssessmentRequest request = buildRequest(application, creditProfile);

        RiskAssessmentResponse response;
        try {
            response = riskEngineClient.assessRiskBlocking(request);
        } catch (Exception e) {
            log.error("Failed to call risk engine, using fallback: {}", e.getMessage());
            response = calculateFallbackRisk(application, creditProfile);
        }

        RiskAssessment assessment = RiskAssessment.builder()
                .loanApplication(application)
                .probabilityOfDefault(response.getPd())
                .riskBand(RiskAssessment.RiskBand.valueOf(response.getRiskBand()))
                .confidence(response.getConfidence())
                .modelVersion(response.getModelVersion())
                .features(serializeFeatures(request))
                .build();

        return riskAssessmentRepository.save(assessment);
    }

    @Transactional(readOnly = true)
    public RiskAssessment getAssessment(UUID applicationId) {
        return riskAssessmentRepository.findByLoanApplicationId(applicationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Risk assessment not found for application: " + applicationId));
    }

    private RiskAssessmentRequest buildRequest(LoanApplication application, CreditProfile creditProfile) {
        RiskAssessmentRequest.RiskAssessmentRequestBuilder builder = RiskAssessmentRequest.builder()
                .monthlyIncome(application.getMonthlyIncome())
                .monthlyExpenses(application.getMonthlyExpenses())
                .requestedAmount(application.getRequestedAmount())
                .termMonths(application.getTermMonths())
                .employmentStatus(application.getApplicant().getEmploymentStatus());

        if (creditProfile != null) {
            builder
                    .creditScore(creditProfile.getCreditScore())
                    .debtToIncomeRatio(creditProfile.getDebtToIncomeRatio())
                    .existingLoanCount(creditProfile.getExistingLoanCount())
                    .totalExistingDebt(creditProfile.getTotalExistingDebt())
                    .creditHistoryMonths(creditProfile.getCreditHistoryMonths())
                    .missedPaymentsLast12Months(creditProfile.getMissedPaymentsLast12Months())
                    .defaultsLast5Years(creditProfile.getDefaultsLast5Years());
        }

        return builder.build();
    }

    private RiskAssessmentResponse calculateFallbackRisk(LoanApplication application, CreditProfile creditProfile) {
        // Simple heuristic-based fallback when risk engine is unavailable
        BigDecimal pd;
        String riskBand;

        if (creditProfile == null) {
            pd = BigDecimal.valueOf(0.15);
            riskBand = "C";
        } else {
            int score = creditProfile.getCreditScore() != null ? creditProfile.getCreditScore() : 500;
            BigDecimal dti = creditProfile.getDebtToIncomeRatio() != null ?
                    creditProfile.getDebtToIncomeRatio() : BigDecimal.valueOf(30);

            if (score >= 700 && dti.compareTo(BigDecimal.valueOf(30)) <= 0) {
                pd = BigDecimal.valueOf(0.03);
                riskBand = "A";
            } else if (score >= 650 && dti.compareTo(BigDecimal.valueOf(40)) <= 0) {
                pd = BigDecimal.valueOf(0.07);
                riskBand = "B";
            } else if (score >= 600 && dti.compareTo(BigDecimal.valueOf(50)) <= 0) {
                pd = BigDecimal.valueOf(0.15);
                riskBand = "C";
            } else if (score >= 500) {
                pd = BigDecimal.valueOf(0.25);
                riskBand = "D";
            } else {
                pd = BigDecimal.valueOf(0.40);
                riskBand = "E";
            }
        }

        return RiskAssessmentResponse.builder()
                .pd(pd)
                .riskBand(riskBand)
                .confidence(BigDecimal.valueOf(0.70))
                .modelVersion("fallback-v1")
                .build();
    }

    private String serializeFeatures(RiskAssessmentRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize features: {}", e.getMessage());
            return null;
        }
    }
}
