package com.loan.decision.loanintake.repository;

import com.loan.decision.loanintake.model.LoanApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LoanApplicationRepository extends JpaRepository<LoanApplication, UUID> {

    List<LoanApplication> findByApplicantId(UUID applicantId);

    List<LoanApplication> findByStatus(LoanApplication.ApplicationStatus status);
}
