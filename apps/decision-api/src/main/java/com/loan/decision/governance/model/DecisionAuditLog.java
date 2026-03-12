package com.loan.decision.governance.model;

import com.loan.decision.decisioning.model.Decision;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "decision_audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DecisionAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decision_id", nullable = false)
    private Decision decision;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType eventType;

    @Column(columnDefinition = "TEXT")
    private String eventDescription;

    private String previousState;

    private String newState;

    @Column(columnDefinition = "TEXT")
    private String ruleEvaluationSummary;

    @Column(columnDefinition = "TEXT")
    private String riskAssessmentSummary;

    @Column(columnDefinition = "TEXT")
    private String ruleResultsJson;

    @Column(columnDefinition = "TEXT")
    private String riskAssessmentJson;

    private String performedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime eventTimestamp;

    @PrePersist
    protected void onCreate() {
        eventTimestamp = LocalDateTime.now();
        if (performedBy == null) {
            performedBy = "SYSTEM";
        }
    }

    public enum EventType {
        DECISION_MADE,
        DECISION_OVERRIDDEN,
        DECISION_REVIEWED,
        STATUS_CHANGED
    }
}
