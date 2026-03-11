package com.loan.decision.creditprofile.service;

import com.loan.decision.creditprofile.model.CreditProfile;
import com.loan.decision.creditprofile.repository.CreditProfileRepository;
import com.loan.decision.loanintake.model.LoanApplication;
import com.loan.decision.loanintake.repository.LoanApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditProfileService {

    private final CreditProfileRepository creditProfileRepository;
    private final LoanApplicationRepository loanApplicationRepository;

    @Transactional
    public CreditProfile captureProfile(UUID applicationId, CreditProfileData data) {
        log.info("Capturing credit profile for application: {}", applicationId);

        LoanApplication application = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));

        if (creditProfileRepository.existsByLoanApplicationId(applicationId)) {
            throw new IllegalStateException("Credit profile already exists for application: " + applicationId);
        }

        BigDecimal dti = calculateDebtToIncomeRatio(
                data.getMonthlyDebtPayments(),
                application.getMonthlyIncome()
        );

        CreditProfile profile = CreditProfile.builder()
                .loanApplication(application)
                .creditScore(data.getCreditScore())
                .existingLoanCount(data.getExistingLoanCount())
                .totalExistingDebt(data.getTotalExistingDebt())
                .monthlyDebtPayments(data.getMonthlyDebtPayments())
                .creditHistoryMonths(data.getCreditHistoryMonths())
                .missedPaymentsLast12Months(data.getMissedPaymentsLast12Months())
                .defaultsLast5Years(data.getDefaultsLast5Years())
                .debtToIncomeRatio(dti)
                .build();

        return creditProfileRepository.save(profile);
    }

    @Transactional(readOnly = true)
    public CreditProfile getProfile(UUID applicationId) {
        return creditProfileRepository.findByLoanApplicationId(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Credit profile not found for application: " + applicationId));
    }

    private BigDecimal calculateDebtToIncomeRatio(BigDecimal monthlyDebt, BigDecimal monthlyIncome) {
        if (monthlyIncome == null || monthlyIncome.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(100);
        }
        if (monthlyDebt == null) {
            return BigDecimal.ZERO;
        }
        return monthlyDebt.divide(monthlyIncome, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CreditProfileData {
        private Integer creditScore;
        private Integer existingLoanCount;
        private BigDecimal totalExistingDebt;
        private BigDecimal monthlyDebtPayments;
        private Integer creditHistoryMonths;
        private Integer missedPaymentsLast12Months;
        private Integer defaultsLast5Years;
    }
}
