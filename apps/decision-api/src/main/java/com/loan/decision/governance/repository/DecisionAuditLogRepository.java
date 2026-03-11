package com.loan.decision.governance.repository;

import com.loan.decision.governance.model.DecisionAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface DecisionAuditLogRepository extends JpaRepository<DecisionAuditLog, UUID> {

    List<DecisionAuditLog> findByDecisionIdOrderByEventTimestampDesc(UUID decisionId);

    List<DecisionAuditLog> findByDecisionLoanApplicationIdOrderByEventTimestampDesc(UUID applicationId);

    List<DecisionAuditLog> findByEventType(DecisionAuditLog.EventType eventType);

    List<DecisionAuditLog> findByEventTimestampBetween(LocalDateTime start, LocalDateTime end);
}
