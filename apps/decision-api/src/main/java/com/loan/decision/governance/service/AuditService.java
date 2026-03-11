package com.loan.decision.governance.service;

import com.loan.decision.governance.model.DecisionAuditLog;
import com.loan.decision.governance.repository.DecisionAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final DecisionAuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public List<DecisionAuditLog> getAuditTrail(UUID applicationId) {
        log.info("Retrieving audit trail for application: {}", applicationId);
        return auditLogRepository.findByDecisionLoanApplicationIdOrderByEventTimestampDesc(applicationId);
    }

    @Transactional(readOnly = true)
    public List<DecisionAuditLog> getAuditLogsByDecision(UUID decisionId) {
        return auditLogRepository.findByDecisionIdOrderByEventTimestampDesc(decisionId);
    }
}
