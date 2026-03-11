package com.loan.decision.rules.model;

import com.loan.decision.loanintake.model.LoanApplication;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "rule_evaluations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleEvaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_application_id", nullable = false)
    private LoanApplication loanApplication;

    @Column(nullable = false)
    private String ruleCode;

    @Column(nullable = false)
    private boolean passed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RuleDefinition.RuleSeverity severity;

    private String reason;

    private String evaluatedCondition;

    @Column(columnDefinition = "TEXT")
    private String actualValue;

    @Column(nullable = false, updatable = false)
    private LocalDateTime evaluatedAt;

    @PrePersist
    protected void onCreate() {
        evaluatedAt = LocalDateTime.now();
    }
}
