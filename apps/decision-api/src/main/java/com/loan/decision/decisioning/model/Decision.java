package com.loan.decision.decisioning.model;

import com.loan.decision.loanintake.model.LoanApplication;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "decisions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Decision {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_application_id", nullable = false, unique = true)
    private LoanApplication loanApplication;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DecisionOutcome outcome;

    @Column(columnDefinition = "TEXT")
    private String reasonCodes;

    private String riskBand;

    private Integer hardRuleFailures;

    private Integer softRuleFailures;

    @Column(columnDefinition = "TEXT")
    private String decisionSummary;

    @Column(nullable = false, updatable = false)
    private LocalDateTime decidedAt;

    private String decidedBy;

    @PrePersist
    protected void onCreate() {
        decidedAt = LocalDateTime.now();
        if (decidedBy == null) {
            decidedBy = "SYSTEM";
        }
    }

    public enum DecisionOutcome {
        APPROVED,
        DECLINED,
        CONDITIONAL,
        PENDING_REVIEW
    }
}
