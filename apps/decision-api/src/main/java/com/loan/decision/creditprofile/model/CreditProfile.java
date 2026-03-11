package com.loan.decision.creditprofile.model;

import com.loan.decision.loanintake.model.LoanApplication;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "credit_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_application_id", nullable = false, unique = true)
    private LoanApplication loanApplication;

    private Integer creditScore;

    private Integer existingLoanCount;

    private BigDecimal totalExistingDebt;

    private BigDecimal monthlyDebtPayments;

    private Integer creditHistoryMonths;

    @Column(name = "missed_payments_last_12_months")
    private Integer missedPaymentsLast12Months;

    @Column(name = "defaults_last_5_years")
    private Integer defaultsLast5Years;

    private BigDecimal debtToIncomeRatio;

    @Column(nullable = false, updatable = false)
    private LocalDateTime capturedAt;

    @PrePersist
    protected void onCreate() {
        capturedAt = LocalDateTime.now();
    }
}
