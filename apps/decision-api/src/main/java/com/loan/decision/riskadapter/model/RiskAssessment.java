package com.loan.decision.riskadapter.model;

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
@Table(name = "risk_assessments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_application_id", nullable = false, unique = true)
    private LoanApplication loanApplication;

    @Column(nullable = false, precision = 10, scale = 6)
    private BigDecimal probabilityOfDefault;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskBand riskBand;

    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal confidence;

    @Column(columnDefinition = "TEXT")
    private String modelVersion;

    @Column(columnDefinition = "TEXT")
    private String modelType;

    @Column(columnDefinition = "TEXT")
    private String features;

    @Column(nullable = false, updatable = false)
    private LocalDateTime assessedAt;

    @PrePersist
    protected void onCreate() {
        assessedAt = LocalDateTime.now();
    }

    public enum RiskBand {
        A,  // Very low risk (0-5% PD)
        B,  // Low risk (5-10% PD)
        C,  // Medium risk (10-20% PD)
        D,  // High risk (20-35% PD)
        E   // Very high risk (>35% PD)
    }
}
